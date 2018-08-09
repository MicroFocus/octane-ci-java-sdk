package com.hp.octane.integrations.api;

public interface SonarService {

    String registerWebhook(String projectKey);

    void unregisterWebhook(String webhookKey);

    void injectSonarDataToOctane(String projectKey, String ciIdentity, String jobId, String buildId);






}
