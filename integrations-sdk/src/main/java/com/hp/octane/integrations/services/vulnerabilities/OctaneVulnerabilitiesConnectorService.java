package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.services.rest.RestService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * handling fetching and pushing vulnerabilities related data from and to octane.
 */

public interface OctaneVulnerabilitiesConnectorService {

    /**
     * Service instance producer - for internal usage only (protected by inaccessible configurer)
     *
     * @return initialized service
     */
    static OctaneVulnerabilitiesConnectorService newInstance(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
        return new OctaneVulnerabilitiesConnectorServiceImpl(configurer, restService);
    }

    /**
     *  push vulnerabilities to octane
     *
     * @param vulnerabilities
     * @param jobId
     * @param buildId
     * @throws IOException
     */
    void pushVulnerabilities(InputStream vulnerabilities, String jobId, String buildId) throws IOException;


    /**
     *
     * @param vulnerabilitiesQueueItem
     * @param remoteTag
     * @return
     * @throws IOException
     */
    List<String> getRemoteIdsOfExistIssuesFromOctane(VulnerabilitiesQueueItem vulnerabilitiesQueueItem, String remoteTag) throws IOException;

    /**
     * return beseline date from which to start to fetch vul
     *
     * @param jobId
     * @param buildId
     * @return
     * @throws IOException
     */
     OctaneResponse getBaselineDateFromOctane(String jobId, String buildId) throws IOException;
}


