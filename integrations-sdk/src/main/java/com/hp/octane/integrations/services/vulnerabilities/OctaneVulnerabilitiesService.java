package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.rest.RestService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

public interface OctaneVulnerabilitiesService {

     static OctaneVulnerabilitiesService newInstance(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
          return new OctaneVulnerabilitiesServiceImpl(configurer,restService);
     }

     void pushVulnerabilities(InputStream vulnerabilities, String jobId, String buildId) throws IOException;

     List<String> getRemoteIdsOfExistIssuesFromOctane(VulnerabilitiesQueueItem vulnerabilitiesQueueItem, String remoteTag) throws IOException;

     Date vulnerabilitiesPreflightRequest(String jobId, String buildId) throws IOException;
}


