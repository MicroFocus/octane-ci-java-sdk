package com.hp.octane.integrations.services.coverage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.api.RestService;
import com.hp.octane.integrations.api.SonarService;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.coverage.BuildCoverage;
import com.hp.octane.integrations.exceptions.OctaneSDKSonarException;
import com.hp.octane.integrations.services.logs.LogsServiceImpl;
import com.hp.octane.integrations.services.queue.PermanentQueueItemException;
import com.hp.octane.integrations.services.queue.QueueService;
import com.hp.octane.integrations.services.queue.TemporaryQueueItemException;
import com.hp.octane.integrations.util.CIPluginSDKUtils;
import com.squareup.tape.ObjectQueue;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.hp.octane.integrations.api.RestService.*;


public class SonarServiceImpl extends OctaneSDK.SDKServiceBase implements SonarService {


    private static String WEBHOOK_CREATE_URI = "/api/webhooks/create";
    private static String WEBHOOK_DELETE_URI = "/api/webhooks/delete";
    private static String WEBHOOK_LIST_URI = "/api/webhooks/list";
    private static String SONAR_STATUS_URI = "/api/system/status";
    private static String CONNECTION_FAILURE = "CONNECTION_FAILURE";

    public static String COMPONENT_TREE_URI = "/api/measures/component_tree";
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();
    private RestService restService;
    private static final Logger logger = LogManager.getLogger(SonarServiceImpl.class);

    private static final String BUILD_COVERAGE_QUEUE_FILE = "build-coverage-queue.dat";


    private final ExecutorService worker = Executors.newSingleThreadExecutor(new SonarServiceImpl.BuildCoveragePushWorkerThreadFactory());
    private final ObjectQueue<SonarServiceImpl.BuildCoverageQueueItem> buildCoverageQueue;

    private int TEMPORARY_ERROR_BREATHE_INTERVAL = 15000;
    private int LIST_EMPTY_INTERVAL = 3000;


    static ObjectMapper objectMapper = new ObjectMapper();


    public SonarServiceImpl(Object internalUsageValidator, QueueService queueService, RestService restService) {
        super(internalUsageValidator);

        if (queueService == null) {
            throw new IllegalArgumentException("queue service MUST NOT be null");
        }
        if (restService == null) {
            throw new IllegalArgumentException("rest service MUST NOT be null");
        }

        if (queueService.isPersistenceEnabled()) {
            buildCoverageQueue = queueService.initFileQueue(BUILD_COVERAGE_QUEUE_FILE, SonarServiceImpl.BuildCoverageQueueItem.class);
        } else {
            buildCoverageQueue = queueService.initMemoQueue();
        }

        this.restService = restService;

        logger.info("starting background worker...");
        startBackgroundWorker();
        logger.info("initialized SUCCESSFULLY (backed by " + buildCoverageQueue.getClass().getSimpleName() + ")");
    }

    private void startBackgroundWorker() {
        worker.execute(new Runnable() {
            public void run() {
                while (true) {
                    if (buildCoverageQueue.size() > 0) {
                        SonarServiceImpl.BuildCoverageQueueItem buildCoverageQueueItem = null;
                        try {
                            buildCoverageQueueItem = buildCoverageQueue.peek();
                            pushSonarDataToOctane(pluginServices.getServerInfo().getInstanceId(), buildCoverageQueueItem);
                            logger.debug("successfully processed " + buildCoverageQueueItem);
                            buildCoverageQueue.remove();
                        } catch (Throwable t) {
                            logger.error(new StringBuilder().append("unexpected error on build log item '")
                                    .append(buildCoverageQueueItem)
                                    .append("', passing over").toString(), t);
                            buildCoverageQueue.remove();
                        }
                    } else {
                        breathe(LIST_EMPTY_INTERVAL);
                    }
                }
            }
        });
    }

    private void breathe(int period) {
        try {
            Thread.sleep(period);
        } catch (InterruptedException ie) {
            logger.error("interrupted while breathing", ie);
        }
    }

    @Override
    public synchronized void ensureWebhookExist(String ciCallbackUrl, String sonarURL, String sonarToken) throws OctaneSDKSonarException {


        //problem in sonar project key in new project
        try {
            String webhookKey = getWebhookKey(ciCallbackUrl,sonarURL,sonarToken);
            if (webhookKey == null) {
                HttpClient httpClient = HttpClientBuilder.create().build();

                URIBuilder uriBuilder = new URIBuilder(sonarURL + WEBHOOK_CREATE_URI)
                        .setParameter("name", "ci_" + pluginServices.getServerInfo().getInstanceId())
                        .setParameter("url", ciCallbackUrl);

                HttpPost request = new HttpPost(uriBuilder.toString());
                setTokenInHttpRequest(request, sonarToken);
                HttpResponse response = httpClient.execute(request);

                JsonNode jsonResponse = objectMapper.readTree(response.getEntity().getContent());
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    String errorMessage = new StringBuilder()
                            .append("exception during webhook registration for  ciNotificationUrl: ").append(ciCallbackUrl)
                            .append(" with status code: ").append(response.getStatusLine().getStatusCode())
                            .append(" with errors: ").append(jsonResponse.get("errors").toString()).toString();
                    throw new OctaneSDKSonarException(errorMessage);
                }
            }

        } catch (OctaneSDKSonarException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            String errorMessage = new StringBuilder()
                    .append("exception during webhook registration for ciNotificationUrl: ").append(ciCallbackUrl)
                    .toString();

            logger.error(errorMessage, e);
            throw new OctaneSDKSonarException(errorMessage, e);
        }
    }

    @Override
    public void enqueueFetchAndPushSonarCoverageToOctane(String jobId, String buildId, String projectKey, String sonarURL, String sonarToken){
        buildCoverageQueue.add(new SonarServiceImpl.BuildCoverageQueueItem(jobId, buildId, projectKey, sonarURL, sonarToken));
    }


    @Override
    public String getSonarStatus(String sonarURL) {

        try {
            URIBuilder uriBuilder = new URIBuilder(sonarURL + SONAR_STATUS_URI);

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(uriBuilder.build());
            HttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return objectMapper.readTree(response.getEntity().getContent()).get("status").textValue();
            } else {
                return CONNECTION_FAILURE;
            }
        } catch (Exception e) {
            return CONNECTION_FAILURE;
        }
    }


    public void pushSonarDataToOctane(String serverId, BuildCoverageQueueItem queueItem) throws OctaneSDKSonarException {

        OctaneConfiguration octaneConfiguration = pluginServices.getOctaneConfiguration();
        if (octaneConfiguration == null || !octaneConfiguration.isValid()) {
            logger.warn("no (valid) Octane configuration found, bypassing " + queueItem);
            return;
        }

        //  preflight
        String[] workspaceIDs = preflightRequest(octaneConfiguration, serverId, queueItem.jobId);
        if (workspaceIDs.length == 0) {
            logger.info("coverage of " + queueItem + " found no interested workspace in Octane, passing over");
            return;
        } else {
            logger.info("coverage of " + queueItem + " found " + workspaceIDs.length + " interested workspace/s in Octane, dispatching the coverage");
        }


        StringBuilder errorMessage = new StringBuilder()
                .append("failed to inject sonarqube coverage data to octane for project key: ")
                .append(queueItem.projectKey)
                .append(" with ciIdentity: ").append(serverId)
                .append(" with jobId: ").append(queueItem.jobId)
                .append(" with buildId: ").append(queueItem.buildId);

        try {

            Integer pageIndex = 0;

            BuildCoverage buildCoverageReport = dtoFactory.newDTO(BuildCoverage.class);

            InputStream reportStream;
            do {
                pageIndex++;
                reportStream = getPageFromSonar(queueItem, pageIndex);
                buildCoverageReport.mergeSonarCoverageReport(reportStream);
            } while (coverageReportHasAnotherPage(pageIndex, reportStream));

            OctaneRequest coveragePutRequest = buildCoveragePutRequest(buildCoverageReport, serverId, queueItem.jobId, queueItem.buildId);
            OctaneResponse response = restService.obtainClient().execute(coveragePutRequest);

            if (response.getStatus() != HttpStatus.SC_OK) {
                errorMessage.append(" with status code: ").append(response.getStatus())
                        .append(" and response body: ").append(response.getBody());
                throw new OctaneSDKSonarException(errorMessage.toString());
            }

        } catch (OctaneSDKSonarException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error(errorMessage.toString(), e);
            throw new OctaneSDKSonarException(errorMessage.toString(), e);
        }
    }

    private  String[] preflightRequest(OctaneConfiguration octaneConfiguration, String serverId, String jobId) {
        String[] result = new String[0];
        OctaneResponse response;

        //  get result
        try {
            OctaneRequest preflightRequest = dtoFactory.newDTO(OctaneRequest.class)
                    .setMethod(HttpMethod.GET)
                    .setUrl(getAnalyticsContextPath(octaneConfiguration.getUrl(), octaneConfiguration.getSharedSpace()) +
                            "servers/" + serverId + "/jobs/" + jobId + "/workspaceId");
            response = restService.obtainClient().execute(preflightRequest);
            if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
                throw new TemporaryQueueItemException("preflight request failed with status " + response.getStatus());
            } else if (response.getStatus() != HttpStatus.SC_OK && response.getStatus() != HttpStatus.SC_NO_CONTENT) {
                throw new PermanentQueueItemException("preflight request failed with status " + response.getStatus());
            }
        } catch (IOException ioe) {
            throw new TemporaryQueueItemException(ioe);
        }

        //  parse result
        if (response.getBody() != null && !response.getBody().isEmpty()) {
            try {
                result = CIPluginSDKUtils.getObjectMapper().readValue(response.getBody(), String[].class);
            } catch (IOException ioe) {
                throw new PermanentQueueItemException("failed to parse preflight response '" + response.getBody() + "' for '" + jobId + "'");
            }
        }
        return result;
    }


    private Boolean coverageReportHasAnotherPage(Integer pageIndex, InputStream content) throws IOException {
        JsonNode jsonContent = objectMapper.readTree(content);

        JsonNode pagingNode = jsonContent.get("paging");
        Integer pageSize = pagingNode.get("pageSize").intValue();
        Integer total = pagingNode.get("total").intValue();
        return pageSize * pageIndex < total;
    }

    private OctaneRequest buildCoveragePutRequest(BuildCoverage buildCoverage, String ciIdentity, String jobId, String buildId) throws URISyntaxException, JsonProcessingException {

        URIBuilder uriBuilder = new URIBuilder(getAnalyticsContextPath(pluginServices.getOctaneConfiguration().getUrl(), pluginServices.getOctaneConfiguration().getSharedSpace()) + "coverage")
                .setParameter("ci-server-identity", ciIdentity)
                .setParameter("ci-job-id", jobId)
                .setParameter("ci-build-id", buildId)
                .setParameter("file-type", SonarService.SONAR_TYPE);

        String reportToOctane = objectMapper.writeValueAsString(buildCoverage);
        OctaneRequest coverageRequest = dtoFactory.newDTO(OctaneRequest.class)
                .setMethod(HttpMethod.PUT)
                .setUrl(uriBuilder.toString())
                .setBody(reportToOctane);
        return coverageRequest;
    }


    public String getWebhookKey(String ciNotificationUrl,  String sonarURL, String token) throws OctaneSDKSonarException {

        try {
            URIBuilder uriBuilder = new URIBuilder(sonarURL + WEBHOOK_LIST_URI);

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(uriBuilder.build());
            setTokenInHttpRequest(request, token);

            HttpResponse response = httpClient.execute(request);
            JsonNode jsonResponse = objectMapper.readTree(response.getEntity().getContent());
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                ArrayNode webhooksListJson = (ArrayNode) jsonResponse.get("webhooks");
                if (webhooksListJson.size() > 0) {
                    for (JsonNode webhookNode : webhooksListJson) {
                        String entryURL = webhookNode.get("url").textValue();
                        if (entryURL.equals(ciNotificationUrl)) {
                            return webhookNode.get("key").textValue();
                        }
                    }
                }
                return null;

            } else {
                String errorMessage = new StringBuilder()
                        .append("failed to get webhook key from soanrqube with notification URL: ")
                        .append(ciNotificationUrl)
                        .append(" with status code: ").append(response.getStatusLine().getStatusCode())
                        .append(" with errors: ").append(jsonResponse.get("errors").toString()).toString();
                throw new OctaneSDKSonarException(errorMessage);

            }
        } catch (OctaneSDKSonarException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            String errorMessage = new StringBuilder()
                    .append("failed to get webhook key from soanrqube with notification URL: ").append(ciNotificationUrl).toString();
            logger.error(errorMessage, e);
            throw new OctaneSDKSonarException(errorMessage, e);
        }
    }


    private InputStream  getPageFromSonar( BuildCoverageQueueItem queueItem, Integer page) throws OctaneSDKSonarException {
        String sonarURL = queueItem.sonarURL;
        String token = queueItem.sonarToken;
        String projectKey = queueItem.projectKey;
        try {

            URIBuilder uriBuilder = new URIBuilder(sonarURL + COMPONENT_TREE_URI);
            uriBuilder.setParameter("metricKeys", "lines_to_cover,uncovered_lines")
                    .setParameter("component", projectKey)
                    .setParameter("qualifiers", "FIL,TRK")
                    .setParameter("ps", "500")
                    .setParameter("p", page.toString());

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(uriBuilder.build());
            setTokenInHttpRequest(request, token);

            HttpResponse httpResponse = httpClient.execute(request);
            return httpResponse.getEntity().getContent();

        } catch (Exception e) {

            String errorMessage = new StringBuilder()
                    .append("failed to get coverage data from sonar for project: ")
                    .append(projectKey).toString();
            logger.error(errorMessage, e);
            throw new OctaneSDKSonarException(errorMessage, e);
        }
    }

    private void setTokenInHttpRequest(HttpRequest request, String token) throws AuthenticationException {
        UsernamePasswordCredentials creds
                = new UsernamePasswordCredentials(token, "");
        request.addHeader(new BasicScheme().authenticate(creds, request, null));
    }

    private static final class BuildCoverageQueueItem implements QueueService.QueueItem {
        private String jobId;
        private String buildId;
        private String projectKey;
        private  String sonarURL;
        private String sonarToken;


        //  [YG] this constructor MUST be present, don't remove
        private BuildCoverageQueueItem() {
        }

        public BuildCoverageQueueItem(String jobId, String buildId, String projectKey, String sonarURL, String sonarToken) {
            this.jobId = jobId;
            this.buildId = buildId;
            this.projectKey = projectKey;
            this.sonarURL = sonarURL;
            this.sonarToken = sonarToken;
        }


        @Override
        public String toString() {
            return "'" + jobId + " #" + buildId + "'";
        }
    }

    private static final class BuildCoveragePushWorkerThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread result = new Thread(runnable);
            result.setName("BuildCoveragePushWorker-" + result.getId());
            result.setDaemon(true);
            return result;
        }
    }


    private String getAnalyticsContextPath(String octaneBaseUrl, String sharedSpaceId) {
        return octaneBaseUrl + SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + ANALYTICS_CI_PATH_PART;
    }
}
