/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.hp.octane.integrations.services.bridge;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.*;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.dto.general.CIServerTypes;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.configuration.ConfigurationServiceImpl;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.tasking.TasksProcessor;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Bridge Service meant to provide an abridged connection functionality
 * Handled by:
 * com.hp.mqm.analytics.common.resources.CIAnalyticsCommonSSAResource#getAbridgedTaskAsync
 */

final class BridgeServiceImpl implements BridgeService {
    private static final Logger logger = LogManager.getLogger(BridgeServiceImpl.class);
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();

    private final ExecutorService connectivityExecutors = Executors.newFixedThreadPool(5, new AbridgedConnectivityExecutorsFactory());
    private final ExecutorService taskProcessingExecutors = Executors.newFixedThreadPool(10, new AbridgedTasksExecutorsFactory());

    private final OctaneSDK.SDKServicesConfigurer configurer;
    private final RestService restService;
    private final ConfigurationService configurationService;
    private final TasksProcessor tasksProcessor;
    private long lastLogTime = 0;
    private final static long MILLI_TO_HOUR = 1000 * 60 * 60;
    private long continuousExceptionsCounter = 0;
    private long forcedGetOctaneConnectivityStatusCalls = 0;

    //Metrics
    private long lastRequestToOctaneTime = 0;
    private ServiceState serviceState = ServiceState.Initial;
    private long stateStartTime = 0;
    private long requestTimeoutCount = 0;
    private long lastRequestTimeoutTime = 0;

    BridgeServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService, TasksProcessor tasksProcessor, ConfigurationService configurationService) {
        if (configurer == null) {
            throw new IllegalArgumentException("invalid configurer");
        }
        if (restService == null) {
            throw new IllegalArgumentException("rest service MUST NOT be null");
        }
        if (tasksProcessor == null) {
            throw new IllegalArgumentException("task processor MUST NOT be null");
        }
        if (configurationService == null) {
            throw new IllegalArgumentException("configuration service MUST NOT be null");
        }

        this.configurationService = configurationService;
        this.configurer = configurer;
        this.restService = restService;
        this.tasksProcessor = tasksProcessor;

        logger.info(configurer.octaneConfiguration.getLocationForLog() + "starting background worker...");
        connectivityExecutors.execute(this::worker);
        logger.info(configurer.octaneConfiguration.getLocationForLog() + "initialized SUCCESSFULLY");
    }

    @Override
    public Map<String, Object> getMetrics() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("isShutdown", isShutdown());
        map.put("state", serviceState.name());
        map.put("stateStartTime", new Date(stateStartTime));
        map.put("lastRequestToOctaneTime", new Date(lastRequestToOctaneTime));
        map.put("connectivityExecutors.getActiveCount", ((ThreadPoolExecutor) connectivityExecutors).getActiveCount());
        map.put("requestTimeoutCount", this.requestTimeoutCount);
        map.put("forcedGetOctaneConnectivityStatus.calls", this.forcedGetOctaneConnectivityStatusCalls);
        map.put("continuousExceptionsCounter", this.continuousExceptionsCounter);

        if (lastRequestTimeoutTime > 0) {
            map.put("lastRequestTimeoutTime", new Date(lastRequestTimeoutTime));
        }
        return map;
    }

    @Override
    public void shutdown() {
        logger.info(configurer.octaneConfiguration.getLocationForLog() + "shutdown");
        connectivityExecutors.shutdown();
        taskProcessingExecutors.shutdown();
        changeServiceState(ServiceState.Closed);
    }

    @Override
    public boolean isShutdown() {
        return connectivityExecutors.isShutdown() || taskProcessingExecutors.isShutdown();
    }

    //  infallible everlasting background worker
    private void worker() {
        try {
            String tasksJSON = null;
            CIServerInfo serverInfo = configurer.pluginServices.getServerInfo();
            CIPluginInfo pluginInfo = configurer.pluginServices.getPluginInfo();
            String client = configurer.octaneConfiguration.getClient();

            // add log about activity once a hour
            if (hoursDifference(System.currentTimeMillis(), lastLogTime) >= 1) {
                String status = "active";
                if (configurer.octaneConfiguration.isSuspended()) {
                    status = "suspended";
                } else if (!configurer.octaneConfiguration.isSdkSupported()) {
                    status = "deactivated (sdk is not supported)";
                }
                logger.info(configurer.octaneConfiguration.getLocationForLog() + "task polling is " + status);
                lastLogTime = System.currentTimeMillis();
            }

            if (configurer.octaneConfiguration.isDisabled()) {
                changeServiceState(ServiceState.Disabled);
                CIPluginSDKUtils.doWait(20 * 1000);//wait 20 sec
            } else {
                //  get tasks, wait if needed and return with task or timeout or error
                tasksJSON = getAbridgedTasks(
                        configurer.octaneConfiguration.getInstanceId(),
                        serverInfo.getType() == null ? CIServerTypes.UNKNOWN.value() : serverInfo.getType(),
                        serverInfo.getUrl() == null ? "" : serverInfo.getUrl(),
                        pluginInfo == null || pluginInfo.getVersion() == null ? "" : pluginInfo.getVersion(),
                        client == null ? "" : client,
                        configurer.octaneConfiguration.getImpersonatedUser() == null ? "" : configurer.octaneConfiguration.getImpersonatedUser());
            }
            //  regardless of response - reconnect again to keep the light on
            if (!connectivityExecutors.isShutdown()) {
                connectivityExecutors.execute(this::worker);
            } else {
                changeServiceState(ServiceState.StopTaskPolling);
                logger.info(configurer.octaneConfiguration.getLocationForLog() + "Shutdown flag is up - stop task processing");
            }

            //  now can process the received tasks - if any
            if (tasksJSON != null && !tasksJSON.isEmpty()) {
                handleTasks(tasksJSON);
            }
        } catch (Throwable t) {
            try {
                breathingOnException("getting tasks from Octane Server temporary failed", 2, t);
                if (!connectivityExecutors.isShutdown()) {
                    connectivityExecutors.execute(this::worker);
                } else {
                    changeServiceState(ServiceState.StopTaskPolling);
                    logger.info(configurer.octaneConfiguration.getLocationForLog() + "Shutdown flag is up - stop task processing");
                }
            } catch (Throwable t2) {
                logger.error(configurer.octaneConfiguration.getLocationForLog() + "unexpected exception in BridgeServiceImpl.worker", t2);
            }
        }
    }

    private void changeServiceState(ServiceState newState) {
        if (!serviceState.equals(newState)) {
            stateStartTime = System.currentTimeMillis();
        }
        serviceState = newState;

        if (newState.equals(ServiceState.WaitingToOctane)) {
            lastRequestToOctaneTime = System.currentTimeMillis();
        }
        //logger.info(configurer.octaneConfiguration.geLocationForLog() + "State changed to " + newState);
    }

    private String getAbridgedTasks(String selfIdentity, String selfType, String selfUrl, String pluginVersion, String octaneUser, String ciServerUser) {
        String responseBody = null;
        OctaneRestClient octaneRestClient = restService.obtainOctaneRestClient();
        Map<String, String> headers = new HashMap<>();
        headers.put(RestService.ACCEPT_HEADER, ContentType.APPLICATION_JSON.getMimeType());
        OctaneRequest octaneRequest = dtoFactory.newDTO(OctaneRequest.class)
                .setMethod(HttpMethod.GET)
                .setTimeoutSec(60)
                .setUrl(configurer.octaneConfiguration.getUrl() +
                        RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + configurer.octaneConfiguration.getSharedSpace() +
                        RestService.ANALYTICS_CI_PATH_PART + "servers/" + selfIdentity + "/tasks?self-type=" + CIPluginSDKUtils.urlEncodeQueryParam(selfType) +
                        "&self-url=" + CIPluginSDKUtils.urlEncodeQueryParam(selfUrl) +
                        "&api-version=" + OctaneSDK.API_VERSION +
                        "&sdk-version=" + CIPluginSDKUtils.urlEncodeQueryParam(OctaneSDK.SDK_VERSION) +
                        "&plugin-version=" + CIPluginSDKUtils.urlEncodeQueryParam(pluginVersion) +
                        "&client-id=" + CIPluginSDKUtils.urlEncodeQueryParam(octaneUser) +
                        "&ci-server-user=" + CIPluginSDKUtils.urlEncodeQueryParam(ciServerUser))
                .setHeaders(headers);
        try {
            changeServiceState(ServiceState.WaitingToOctane);
            OctaneResponse octaneResponse = octaneRestClient.execute(octaneRequest);
            changeServiceState(ServiceState.AfterWaitingToOctane);
            if (octaneResponse.getStatus() == HttpStatus.SC_OK) {
                responseBody = octaneResponse.getBody();
                if (CIPluginSDKUtils.isServiceTemporaryUnavailable(responseBody)) {
                    breathingOnException("Saas service is temporary unavailable.", 60, null);
                    responseBody = null;
                } else {
                    setConnectionSuccessful();
                }
            } else {
                if (octaneResponse.getStatus() == HttpStatus.SC_NO_CONTENT) {
                    logger.debug(configurer.octaneConfiguration.getLocationForLog() + "no tasks found on server");
                    setConnectionSuccessful();
                } else if (octaneResponse.getStatus() == HttpStatus.SC_REQUEST_TIMEOUT) {
                    logger.debug(configurer.octaneConfiguration.getLocationForLog() + "expected timeout disconnection on retrieval of abridged tasks, reconnecting immediately...");
                    setConnectionSuccessful();
                } else if (octaneResponse.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE || octaneResponse.getStatus() == HttpStatus.SC_BAD_GATEWAY) {
                    breathingOnException("Octane service is unavailable.", 30, null);
                } else if (octaneResponse.getStatus() == HttpStatus.SC_UNAUTHORIZED) {
                    breathingOnException("Connection to Octane failed: authentication error.", 30, null);
                } else if (octaneResponse.getStatus() == HttpStatus.SC_FORBIDDEN) {
                    breathingOnException("Connection to Octane failed: authorization error.", 30, null);
                } else if (octaneResponse.getStatus() == HttpStatus.SC_NOT_FOUND) {
                    breathingOnException("Connection to Octane failed: 404, validate proxy settings, maybe missing 'No Proxy Host' setting?", 30, null);
                } else if (octaneResponse.getStatus() == HttpStatus.SC_TEMPORARY_REDIRECT) {
                    breathingOnException("Task polling request is redirected. Possibly Octane service is unavailable now.", 30, null);
                } else {
                    String output = octaneResponse.getBody() == null ? "" : octaneResponse.getBody().substring(0, Math.min(octaneResponse.getBody().length(), 2000));//don't print more that 2000 characters
                    breathingOnException("Unexpected response from Octane; status: " + octaneResponse.getStatus() + ", content: " + output + ".", 20, null);
                }
            }
        } catch (InterruptedIOException ie) {
            requestTimeoutCount++;
            lastRequestTimeoutTime = System.currentTimeMillis();
            long timeout = (lastRequestTimeoutTime - stateStartTime) / 1000;
            breathingOnException("!!!!!!!!!!!!!!!!!!! request timeout after request timeout after " + timeout + " sec", 5, ie);
        } catch (SocketException | UnknownHostException e) {
            breathingOnException("Failed to retrieve abridged tasks. ALM Octane Server is not accessible", 30, e);
        } catch (IOException ioe) {
            breathingOnException("Failed to retrieve abridged tasks", 30, ioe);
        } catch (Throwable t) {
            breathingOnException("Unexpected error during retrieval of abridged tasks", 30, t);
        }
        return responseBody;
    }

    private void setConnectionSuccessful() {
        ((ConfigurationServiceImpl)configurationService).setConnected(true);
        if (continuousExceptionsCounter > 4) {
            logger.info(configurer.octaneConfiguration.getLocationForLog() + "force getOctaneConnectivityStatus after " + continuousExceptionsCounter + " failed trials");
            ((ConfigurationServiceImpl)configurationService).getOctaneConnectivityStatus(true);
            forcedGetOctaneConnectivityStatusCalls++;
        }
        continuousExceptionsCounter = 0;
    }

    private void breathingOnException(String msg, int secs, Throwable t) {
        ((ConfigurationServiceImpl)configurationService).setConnected(false);
        continuousExceptionsCounter ++;
        String error = (t == null) ? "" : " : " + t.getClass().getCanonicalName() + " - " + t.getMessage();
        logger.error(configurer.octaneConfiguration.getLocationForLog() + msg + error + ". Breathing " + secs + " secs.");
        changeServiceState(ServiceState.PostponingOnException);
        CIPluginSDKUtils.doWait(secs * 1000);
    }

    private void handleTasks(String tasksJSON) {
        try {
            logger.info(configurer.octaneConfiguration.getLocationForLog() + "parsing tasks...");
            OctaneTaskAbridged[] tasks = dtoFactory.dtoCollectionFromJson(tasksJSON, OctaneTaskAbridged[].class);
            logger.info(configurer.octaneConfiguration.getLocationForLog() + "parsed " + tasks.length + " tasks, processing...");
            for (final OctaneTaskAbridged task : tasks) {
                if (taskProcessingExecutors.isShutdown()) {
                    break;
                }
                taskProcessingExecutors.execute(() -> {
                    OctaneResultAbridged result = tasksProcessor.execute(task);
                    int submitStatus = putAbridgedResult(
                            configurer.octaneConfiguration.getInstanceId(),
                            result.getId(),
                            dtoFactory.dtoToJsonStream(result), false);
                    logger.info(configurer.octaneConfiguration.getLocationForLog() + "result for task '" + result.getId() + "' submitted with status " + submitStatus);
                });
            }
        } catch (Exception e) {
            logger.error(configurer.octaneConfiguration.getLocationForLog() + "failed to process tasks", e);
        }
    }

    private int putAbridgedResult(String selfIdentity, String taskId, InputStream contentJSON, boolean rerun) {
        OctaneRestClient octaneRestClientImpl = restService.obtainOctaneRestClient();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(RestService.CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());
        OctaneRequest octaneRequest = dtoFactory.newDTO(OctaneRequest.class)
                .setMethod(HttpMethod.PUT)
                .setUrl(configurer.octaneConfiguration.getUrl() +
                        RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + configurer.octaneConfiguration.getSharedSpace() +
                        RestService.ANALYTICS_CI_PATH_PART + "servers/" + selfIdentity + "/tasks/" + taskId + "/result")
                .setHeaders(headers)
                .setBody(contentJSON);
        try {
            OctaneResponse octaneResponse = octaneRestClientImpl.execute(octaneRequest);
            return octaneResponse.getStatus();
        } catch (IOException ioe) {
            logger.error("{} failed to submit abridged task's result, rerun = {}", configurer.octaneConfiguration.getLocationForLog(), rerun, ioe);
            if(!rerun) {
                CIPluginSDKUtils.doWait(1000);
                return putAbridgedResult(selfIdentity, taskId, contentJSON, true);
            } else {
                return 0;
            }
        }
    }

    private static final class AbridgedConnectivityExecutorsFactory implements ThreadFactory {
        public Thread newThread(Runnable runnable) {
            Thread result = new Thread(runnable);
            result.setName("AbridgedConnectivityWorker-" + result.getId());
            result.setDaemon(true);
            return result;
        }
    }

    private static final class AbridgedTasksExecutorsFactory implements ThreadFactory {
        public Thread newThread(Runnable runnable) {
            Thread result = new Thread(runnable);
            result.setName("AbridgedTasksWorker-" + result.getId());
            result.setDaemon(true);
            return result;
        }
    }

    private static long hoursDifference(long date1, long date2) {
        return (date1 - date2) / MILLI_TO_HOUR;
    }
}
