package com.hp.octane.integrations.api;

public interface SonarService {

    String registerWebhook(String jenkinsUrl, String sonarURL, String projectKey, String token);

    void unregisterWebhook(String sonarURL, String projectKey, String token);

    void injectSonarDataToOctane(String projectKey, String ciIdentity, String jobId, String buildId);

}
