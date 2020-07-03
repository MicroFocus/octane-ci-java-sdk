package com.hp.octane.integrations.services.scmdata;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.causes.CIEventCauseType;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.scm.SCMData;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.WorkerPreflight;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.hp.octane.integrations.services.events.EventsService;
import com.hp.octane.integrations.services.queueing.QueueingService;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import com.squareup.tape.ObjectQueue;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class SCMDataServiceImpl implements SCMDataService {

    protected final RestService restService;
    private static final Logger logger = LogManager.getLogger(SCMDataServiceImpl.class);
    private static final String SCMDATA_QUEUE_DAT = "scmdata-queue.dat";
    protected final ConfigurationService configurationService;
    protected final EventsService eventsService;
    protected final OctaneSDK.SDKServicesConfigurer configurer;
    private final WorkerPreflight workerPreflight;

    private final ExecutorService scmProcessingExecutor = Executors.newSingleThreadExecutor(new SCMDataServiceImpl.SCMPushWorkerThreadFactory());
    private final ObjectQueue<SCMDataQueueItem> scmDataQueue;
    
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();

    private int TEMPORARY_ERROR_BREATHE_INTERVAL = 10000;
    public static final String SCM_REST_API_SUPPORTED_VERSION = "15.1.20";

    public SCMDataServiceImpl(QueueingService queueingService, OctaneSDK.SDKServicesConfigurer configurer,
                              RestService restService, ConfigurationService configurationService, EventsService eventsService) {

        if (queueingService == null) {
            throw new IllegalArgumentException("queue Service MUST NOT be null");
        }
        if (restService == null) {
            throw new IllegalArgumentException("rest service MUST NOT be null");
        }
        if (configurer == null) {
            throw new IllegalArgumentException("configurer service MUST NOT be null");
        }
        if (configurationService == null) {
            throw new IllegalArgumentException("configuration service MUST NOT be null");
        }

        this.restService = restService;
        this.configurationService = configurationService;
        this.eventsService = eventsService;

        this.configurer = configurer;
        this.workerPreflight = new WorkerPreflight(this, configurationService, logger);

        if (queueingService.isPersistenceEnabled()) {
            scmDataQueue = queueingService.initFileQueue(SCMDATA_QUEUE_DAT, SCMDataQueueItem.class);
        } else {
            scmDataQueue = queueingService.initMemoQueue();
        }

        logger.info(configurer.octaneConfiguration.geLocationForLog() + "starting background worker...");
        scmProcessingExecutor.execute(this::worker);
        logger.info(configurer.octaneConfiguration.geLocationForLog() + "initialized SUCCESSFULLY (backed by " + scmDataQueue.getClass().getSimpleName() + ")");
    }

    @Override
    public void enqueueSCMData(String jobId, String buildId, SCMData scmData) {
        if (this.configurer.octaneConfiguration.isDisabled()) {
            return;
        }

        if( isSCMRestAPI() && configurationService.isOctaneVersionGreaterOrEqual(SCM_REST_API_SUPPORTED_VERSION)) {
            SCMDataQueueItem scmDataQueueItem = new SCMDataQueueItem(jobId, buildId);
            scmDataQueue.add(scmDataQueueItem);
            logger.info(configurer.octaneConfiguration.geLocationForLog() + scmDataQueueItem.getJobId() + " #" + scmDataQueueItem.getBuildId() + " was added to queue");

            workerPreflight.itemAddedToQueue();
        } else {
            pushSCMDataByEvent(scmData, jobId, buildId);
        }
    }

    @Override
    public void shutdown() {
        scmProcessingExecutor.shutdown();
    }

    @Override
    public boolean isShutdown() {
        return scmProcessingExecutor.isShutdown();
    }

    @Override
    public Map<String, Object> getMetrics() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("isShutdown", this.isShutdown());
        map.put("queueSize", this.getQueueSize());
        workerPreflight.addMetrics(map);
        return map;
    }

    @Override
    public long getQueueSize() {
        return scmDataQueue.size();
    }

    @Override
    public void clearQueue() {
        while (scmDataQueue.size() > 0) {
            scmDataQueue.remove();
        }
    }


    private static final class SCMPushWorkerThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread result = new Thread(runnable);
            result.setName("SCMDataPushWorker-" + result.getId());
            result.setDaemon(true);
            return result;
        }
    }

    //  infallible everlasting background worker
    private void worker() {
        while (!scmProcessingExecutor.isShutdown()) {
            if(!workerPreflight.preflight()){
                continue;
            }

            SCMDataQueueItem queueItem = null;
            try {
                queueItem = scmDataQueue.peek();
                processPushSCMDataQueueItem(queueItem);
                scmDataQueue.remove();
            } catch (TemporaryException tque) {
                logger.error(configurer.octaneConfiguration.geLocationForLog() + "temporary error on " + queueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying", tque);
            } catch (PermanentException pqie) {
                logger.error(configurer.octaneConfiguration.geLocationForLog() + "permanent error on " + queueItem + ", passing over", pqie);
                scmDataQueue.remove();
            } catch (Throwable t) {
                logger.error(configurer.octaneConfiguration.geLocationForLog() + "unexpected error on build log item '" + queueItem + "', passing over", t);
                scmDataQueue.remove();
            }
        }
    }


    private boolean processPushSCMDataQueueItem(SCMDataQueueItem queueItem) {

        try {
            InputStream scmData = configurer.pluginServices.getSCMData(queueItem.getJobId(), queueItem.getBuildId());
            if (scmData == null) {
                return false;
            } else {
                pushSCMDataByRestAPI(queueItem.getJobId(), queueItem.getBuildId(), scmData);
                return true;
            }
        } catch (IOException e) {
            throw new PermanentException(e);
        }
    }

    private boolean isEncodeBase64() {
        return ConfigurationParameterFactory.isEncodeCiJobBase64(configurer.octaneConfiguration);
    }

    private boolean isSCMRestAPI() {
        return ConfigurationParameterFactory.isSCMRestAPI(configurer.octaneConfiguration);
    }

    private void pushSCMDataByRestAPI(String jobId, String buildId, InputStream scmData) throws IOException {

        OctaneRestClient octaneRestClient = restService.obtainOctaneRestClient();
        Map<String, String> headers = new HashMap<>();
        headers.put(RestService.CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());

        boolean base64 = isEncodeBase64();
        String encodedJobId = base64 ? CIPluginSDKUtils.urlEncodeBase64(jobId) : CIPluginSDKUtils.urlEncodeQueryParam(jobId);
        String encodedBuildId = CIPluginSDKUtils.urlEncodeQueryParam(buildId);

        String url = getSCMDataContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) +
                "?instance-id=" + configurer.octaneConfiguration.getInstanceId() + "&job-ci-id=" + encodedJobId + "&build-ci-id=" + encodedBuildId;

        if (base64) {
            url = CIPluginSDKUtils.addParameterEncode64ToUrl(url);
        }

        OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
                .setMethod(HttpMethod.PUT)
                .setUrl(url)
                .setHeaders(headers)
                .setBody(scmData);

        OctaneResponse response = octaneRestClient.execute(request);
        if (response.getStatus() == HttpStatus.SC_OK) {
            logger.info(configurer.octaneConfiguration.geLocationForLog() + "scmData for " + jobId + " #" + buildId + ", push SUCCEED : " + response.getBody());
        } else if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
            throw new TemporaryException("scmData push FAILED, service unavailable");
        } else {
            throw new PermanentException("scmData push FAILED, status " + response.getStatus() + "; dropping this item from the queue \n" + response.getBody());
        }
    }

    private void pushSCMDataByEvent(SCMData scmData, String jobId, String buildId) {

        if (jobId == null || jobId.isEmpty())
            throw new IllegalArgumentException("job ID MUST NOT be null nor empty");
        if (buildId == null || buildId.isEmpty())
            throw new IllegalArgumentException("build ID MUST NOT be null nor empty");

        try {
            if (scmData != null) {

                CIEvent event = dtoFactory.newDTO(CIEvent.class)
                        .setEventType(CIEventType.SCM)
                        .setProject(jobId)
                        .setBuildCiId(buildId)
                        .setCauses(generateScmCauses())
                        .setNumber(buildId)
                        .setScmData(scmData);

                eventsService.publishEvent(event);
            }

        } catch (Exception e) {
            logger.error("failed to send SCM event for job " + jobId + " build " + buildId, e);
        }
    }

    private List<CIEventCause> generateScmCauses() {

        CIEventCause scmEventCause = dtoFactory.newDTO(CIEventCause.class);
        scmEventCause.setType(CIEventCauseType.SCM);
        Map<String, CIEventCause> mapScmEventCause = new LinkedHashMap();
        mapScmEventCause.put(scmEventCause.generateKey(), scmEventCause);

        return new ArrayList<>(mapScmEventCause.values());
    }

    private String getSCMDataContextPath(String octaneBaseUrl, String sharedSpaceId) {
        return octaneBaseUrl + RestService.SHARED_SPACE_API_PATH_PART + sharedSpaceId + RestService.SCMDATA_API_PATH_PART;
    }
}
