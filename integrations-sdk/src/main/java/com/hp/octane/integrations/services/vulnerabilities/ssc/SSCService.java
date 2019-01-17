package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.vulnerabilities.VulnerabilitiesToolService;

public interface SSCService extends VulnerabilitiesToolService {

    /**
     * Service instance producer - for internal usage only (protected by inaccessible configurer)
     *
     * @return initialized service
     */
    static SSCService newInstance(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
       return new SSCServiceImpl(configurer,restService);
    }
}