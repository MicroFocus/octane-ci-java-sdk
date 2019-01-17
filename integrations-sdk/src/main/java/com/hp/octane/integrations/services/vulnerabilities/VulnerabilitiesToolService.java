package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.rest.RestService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * represent service handling  specific vulnerabilities Tool (e.g sonarqube, ssc) date fetching, and logic.
 */

public interface VulnerabilitiesToolService {

    /**
     * get Vulnerabilities from external tool
     * @param queueItem
     * @return input stream of json with issues from the vulnerabilities tool rest API
     * @throws IOException
     */
    InputStream getVulnerabilitiesScanResultStream(VulnerabilitiesQueueItem queueItem) throws IOException;

    /**
     * clean up of data retrieved by the vulnerability tool (usallly from cache)
     * @param queueItem
     * @return
     */

    boolean vulnerabilitiesQueueItemCleanUp(VulnerabilitiesQueueItem queueItem);

    RestService getRestService();

    OctaneSDK.SDKServicesConfigurer getConfigurer();

    default List<String> getRemoteIdsOfExistIssuesFromOctane(VulnerabilitiesQueueItem vulnerabilitiesQueueItem, String remoteTag) throws IOException {
        ExistingIssuesInOctane existingIssuesInOctane = new ExistingIssuesInOctane(
                getRestService().obtainOctaneRestClient(),
                getConfigurer().octaneConfiguration);
        return existingIssuesInOctane.getRemoteIdsOpenVulnsFromOctane(vulnerabilitiesQueueItem.getJobId(),
                vulnerabilitiesQueueItem.getBuildId(),
                remoteTag);
    }
}
