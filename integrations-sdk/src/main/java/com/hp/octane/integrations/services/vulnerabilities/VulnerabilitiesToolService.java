package com.hp.octane.integrations.services.vulnerabilities;

import java.io.IOException;
import java.io.InputStream;

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
}
