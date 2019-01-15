package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.vulnerabilities.OctaneVulnerabilitiesService;
import com.hp.octane.integrations.services.vulnerabilities.VulnerabilitiesQueueItem;
import com.hp.octane.integrations.services.vulnerabilities.VulnerabilitiesToolService;

import java.io.InputStream;

public interface SSCService extends VulnerabilitiesToolService {

    static SSCService newInstance(OctaneSDK.SDKServicesConfigurer configurer, RestService restService, OctaneVulnerabilitiesService octaneVulnerabilitiesService) {
        return new SSCServiceImpl(configurer,restService,octaneVulnerabilitiesService);
    }
}