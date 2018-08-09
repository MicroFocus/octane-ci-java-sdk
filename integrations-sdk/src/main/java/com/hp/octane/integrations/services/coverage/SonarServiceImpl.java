package com.hp.octane.integrations.services.coverage;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.api.SonarService;

public class SonarServiceImpl extends OctaneSDK.SDKServiceBase implements SonarService {

    public SonarServiceImpl(Object internalUsageValidator) {
        super(internalUsageValidator);
    }


//    @Override
//    public String registerWebhook(String projectKey) {
//        pluginServices.getSonarInfo().getServerUrl();
//        pluginServices.getSonarInfo().getServerAuthenticationToken();
//        return null;
//    }
//
//    @Override
//    public void unregisterWebhook(String webhookKey) {
//
//    }

    @Override
    public String registerWebhook(String sonarURL, String projectKey, String token) {
        return null;
    }

    @Override
    public void unregisterWebhook(String sonarURL, String projectKey, String token) {

    }

    @Override
    public void injectSonarDataToOctane(String projectKey,String ciIdentity, String jobId, String buildId) {

    }
}
