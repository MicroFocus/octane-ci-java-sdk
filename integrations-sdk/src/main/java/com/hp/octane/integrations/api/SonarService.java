package com.hp.octane.integrations.api;

public interface SonarService {

    String registerWebhook(String jenkinsUrl, String sonarURL, String token, String projectKey);

    void unregisterWebhook(String sonarURL, String token, String projectKey);

    void injectSonarDataToOctane(String projectKey, String ciIdentity, String jobId, String buildId);

    boolean testConnectivity(String projectKey, String sonarUrl, String sonarToken);

    void setSonarAuthentication(String projectKey , String sonarURL, String token);

}
