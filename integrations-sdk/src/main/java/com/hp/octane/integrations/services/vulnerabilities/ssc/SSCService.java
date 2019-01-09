package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.vulnerabilities.VulnerabilitiesQueueItem;

import java.io.InputStream;

public interface SSCService {

    static SSCService newInstance(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
        return new SSCServiceImpl(configurer,restService);
    }

    InputStream getVulnerabilitiesScanResultStream(VulnerabilitiesQueueItem queueItem);

    boolean vulnerabilitiesQueueItemCleanUp(VulnerabilitiesQueueItem queueItem);
}