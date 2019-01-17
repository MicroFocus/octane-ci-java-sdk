package com.hp.octane.integrations.services.vulnerabilities.sonar;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.vulnerabilities.VulnerabilitiesToolService;

public interface SonarVulnerabilitiesService extends VulnerabilitiesToolService {

    static SonarVulnerabilitiesService newInstance (OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
        return new SonarVulnerabilitiesServiceImpl(configurer,restService);
    }

}
