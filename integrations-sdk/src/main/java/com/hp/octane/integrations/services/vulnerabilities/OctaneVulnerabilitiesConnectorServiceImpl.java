package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OctaneVulnerabilitiesConnectorServiceImpl implements OctaneVulnerabilitiesConnectorService {


    private static final Logger logger = LogManager.getLogger(OctaneVulnerabilitiesConnectorServiceImpl.class);
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();
    private OctaneSDK.SDKServicesConfigurer configurer;
    private  RestService restService;

    public OctaneVulnerabilitiesConnectorServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {

        if (configurer == null) {
            throw new IllegalArgumentException("invalid configurer");
        }

        if (restService == null) {
            throw new IllegalArgumentException("rest service MUST NOT be null");
        }

        this.configurer = configurer;
        this.restService = restService;
    }


    @Override
    public void pushVulnerabilities(InputStream vulnerabilities, String jobId, String buildId) throws IOException {
        OctaneRestClient octaneRestClient = restService.obtainOctaneRestClient();
        Map<String, String> headers = new HashMap<>();
        headers.put(RestService.CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());
        String encodedJobId = CIPluginSDKUtils.urlEncodePathParam(jobId);
        String encodedBuildId = CIPluginSDKUtils.urlEncodePathParam(buildId);
        OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
                .setMethod(HttpMethod.POST)
                .setUrl(getVulnerabilitiesContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) +
                        "?instance-id=" + configurer.octaneConfiguration.getInstanceId() + "&job-ci-id=" + encodedJobId + "&build-ci-id=" + encodedBuildId)
                .setHeaders(headers)
                .setBody(vulnerabilities);

        OctaneResponse response = octaneRestClient.execute(request);
        logger.info("vulnerabilities pushed; status: " + response.getStatus() + ", response: " + response.getBody());
        if (response.getStatus() == HttpStatus.SC_ACCEPTED) {
            logger.info("vulnerabilities push SUCCEED for " + jobId + " #" + buildId);
        } else if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
            throw new TemporaryException("vulnerabilities push FAILED, service unavailable");
        } else {
            throw new PermanentException("vulnerabilities push FAILED, status " + response.getStatus() + "; dropping this item from the queue \n" + response.getBody());
        }
    }

    //return octane issues (or null if scan not completed)
   @Override
    public  List<String> getRemoteIdsOfExistIssuesFromOctane(VulnerabilitiesQueueItem vulnerabilitiesQueueItem, String remoteTag) throws IOException {
       ExistingIssuesInOctane existingIssuesInOctane = new ExistingIssuesInOctane(
               restService.obtainOctaneRestClient(),
               configurer.octaneConfiguration);
       return existingIssuesInOctane.getRemoteIdsOpenVulnsFromOctane(vulnerabilitiesQueueItem.getJobId(),
               vulnerabilitiesQueueItem.getBuildId(),
               remoteTag);
    }

    @Override
    public OctaneResponse getBaselineDateFromOctane(String jobId, String buildId) throws IOException {
        String encodedJobId = CIPluginSDKUtils.urlEncodePathParam(jobId);
        String encodedBuildId = CIPluginSDKUtils.urlEncodePathParam(buildId);

        OctaneRequest preflightRequest = dtoFactory.newDTO(OctaneRequest.class)
                .setMethod(HttpMethod.GET)
                .setUrl(getVulnerabilitiesPreFlightContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) +
                        "?instance-id=" + configurer.octaneConfiguration.getInstanceId() + "&job-ci-id=" + encodedJobId + "&build-ci-id=" + encodedBuildId);

        return restService.obtainOctaneRestClient().execute(preflightRequest);
    }

    private String getVulnerabilitiesContextPath(String octaneBaseUrl, String sharedSpaceId) {
        return octaneBaseUrl + RestService.SHARED_SPACE_API_PATH_PART + sharedSpaceId + RestService.VULNERABILITIES;
    }

    private String getVulnerabilitiesPreFlightContextPath(String octaneBaseUrl, String sharedSpaceId) {
        return octaneBaseUrl + RestService.SHARED_SPACE_API_PATH_PART + sharedSpaceId + RestService.VULNERABILITIES_PRE_FLIGHT;
    }

}
