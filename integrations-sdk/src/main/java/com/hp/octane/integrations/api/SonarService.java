package com.hp.octane.integrations.api;

import com.hp.octane.integrations.exceptions.OctaneSDKSonarException;

public interface SonarService {

    String SONAR_TYPE = "SONARQUBBE";

    /**
     * get status object from m sonarQube server listens to sonarURL, as specified in sonar documentation :
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
     * @param sonarURL
     * @return
     */
     String getSonarStatus(String sonarURL);

    /**
     *
     *  enqueue FetchAndPushSonarCoverageToOctane task, the  fetch coverage data for projectKey from sonarQube server associated with sonarURL, and inject it to octane.
     *  the ci parameters will be sent to octane as parameters
     *
     * @param jobId
     * @param buildId
     * @param projectKey
     * @param sonarURL
     * @param sonarToken
     */

     void enqueueFetchAndPushSonarCoverageToOctane(String jobId, String buildId, String projectKey, String sonarURL, String sonarToken);

    /**
     * ensure that webhook with the ciCallbackUrl exist in the soanr server listen to sonarURL, and create new webhook
     * if no webhook with ciCallbackUrl is found
     *
     * @param ciCallbackUrl
     * @param sonarURL
     * @param sonarToken
     * @throws OctaneSDKSonarException
     */

     void ensureWebhookExist(String ciCallbackUrl, String sonarURL, String sonarToken) throws OctaneSDKSonarException;




}
