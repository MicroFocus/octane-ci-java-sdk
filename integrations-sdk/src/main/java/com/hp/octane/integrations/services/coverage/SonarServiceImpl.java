package com.hp.octane.integrations.services.coverage;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.api.SonarService;

public class SonarServiceImpl extends OctaneSDK.SDKServiceBase implements SonarService {

    public SonarServiceImpl(Object internalUsageValidator) {
        super(internalUsageValidator);
    }

    @Override
    public String registerWebhook(String jenkinsUrl, String sonarURL, String projectKey, String token) {
        return null;
    }

    @Override
    public void unregisterWebhook(String sonarURL, String projectKey, String token) {

    }

    @Override
    public void injectSonarDataToOctane(String projectKey,String ciIdentity, String jobId, String buildId) {

    }
}
