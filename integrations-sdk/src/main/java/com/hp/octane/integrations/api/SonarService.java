package com.hp.octane.integrations.api;

import com.hp.octane.integrations.exceptions.OctaneSDKSonarException;

public interface SonarService {

    String SONAR_TYPE = "SONARQUBBE";

    /**
     * set sonar connection details in stateful map, which can have one sonarQube server per object;
     *
     * @param projectKey
     * @param sonarURL
     * @param token
     */
    void setSonarAuthentication(String projectKey, String sonarURL, String token);

    /**
     * register sonarQube webwook with the provided URL, in sonarQube server associated with projectKey
     * if webhook with the same specification already exists, it will be returned instead of creating new one.
     *
     * @param ciNotificationUrl
     * @param projectKey
     * @return unique webhook key
     * @throws OctaneSDKSonarException
     */
    String registerWebhook(String ciNotificationUrl, String projectKey) throws OctaneSDKSonarException;


    /**
     * remove webehook which its UUID key is: webhookKey, in sonarQube server associated with projectKey
     *
     * @param projectKey
     * @param webhookKey
     * @throws OctaneSDKSonarException
     */
    void unregisterWebhook(String projectKey, String webhookKey) throws OctaneSDKSonarException;


    /**
     * fetch coverage data from sonarQube server associated with projectKey, and inject it to octane
     * the ci parameters will be sent to octane as parameters
     *
     * @param projectKey
     * @param ciIdentity
     * @param jobId
     * @param buildId
     * @throws OctaneSDKSonarException
     */
    void injectSonarDataToOctane(String projectKey, String ciIdentity, String jobId, String buildId) throws OctaneSDKSonarException;

    /**
     * get status object from m sonarQube server associated with projectKey, as specified in sonar documentation :
     * <p>
     * STARTING: SonarQube Web Server is up and serving some Web Services (eg. api/system/status) but initialization is still ongoing
     * UP: SonarQube instance is up and running
     * DOWN: SonarQube instance is up but not running because migration has failed (refer to WS /api/system/migrate_db for details) or some other reason (check logs).
     * RESTARTING: SonarQube instance is still up but a restart has been requested (refer to WS /api/system/restart for details).
     * DB_MIGRATION_NEEDED: database migration is required. DB migration can be started using WS /api/system/migrate_db.
     * DB_MIGRATION_RUNNING: DB migration is running (refer to WS /api/system/migrate_db for details)
     * <p>
     * in case of connection failure the status will be:
     * CONNECTION_FAILURE
     *
     * @param projectKey
     * @return
     */
    String getSonarStatus(String projectKey);
}
