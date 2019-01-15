package com.hp.octane.integrations.services.vulnerabilities;

import java.io.InputStream;

public interface VulnerabilitiesToolService {

    InputStream getVulnerabilitiesScanResultStream(VulnerabilitiesQueueItem queueItem);
    boolean vulnerabilitiesQueueItemCleanUp(VulnerabilitiesQueueItem queueItem);
}
