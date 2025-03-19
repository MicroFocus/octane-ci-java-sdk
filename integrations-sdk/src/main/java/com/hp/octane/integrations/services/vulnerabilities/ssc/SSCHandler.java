/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.rest.SSCRestClient;
import com.hp.octane.integrations.services.vulnerabilities.DateUtils;
import com.hp.octane.integrations.services.vulnerabilities.VulnerabilitiesQueueItem;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Artifacts;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.IssueDetails;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Issues;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.ProjectVersions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by hijaziy on 7/23/2018.
 */

public class SSCHandler {
    private final static Logger logger = LogManager.getLogger(SSCHandler.class);
    private SSCProjectConnector sscProjectConnector;
    private ProjectVersions.ProjectVersion projectVersion;
    private long runStartTime;

    public static final String SCAN_RESULT_FILE = "securityScan.json";

    public static final String ARTIFACT_STATUS_COMPLETE = "PROCESS_COMPLETE";
    public static final String ARTIFACT_ERROR_PROCESSING = "ERROR_PROCESSING";

    public boolean isScanProcessFinished() {
        logger.debug("enter isScanProcessFinished");

        Artifacts artifacts = sscProjectConnector.getArtifactsOfProjectVersion(this.projectVersion.id, 10);
        logger.debug("artifacts: " + artifacts.toString());
        Artifacts.Artifact closestArtifact = getClosestArtifact(artifacts);
        if (closestArtifact == null) {
            logger.debug("Cannot find artifact of the run");
            return false;
        }
        if (closestArtifact.status.equals(ARTIFACT_STATUS_COMPLETE)) {
            logger.debug("artifact of the run is in completed");
            return true;
        }
        if (closestArtifact.status.equals(ARTIFACT_ERROR_PROCESSING)) {
            throw new PermanentException("artifact of the run faced error, polling should stop");
        }
        //todo , if there are more cases need to handle separately
        logger.debug("artifact of the run is not complete, polling should continue");
        return false;
    }

    private Artifacts.Artifact getClosestArtifact(Artifacts artifacts) {
        Artifacts.Artifact theCloset = null;
        if (artifacts == null){
            logger.debug("getClosestArtifact artifacts is null");
            return null;

        }
        if(artifacts.getData() == null) {
            logger.debug("getClosestArtifact artifacts.getData() is null");
            return null;
        }
        Date startRunDate = new Date(this.runStartTime);
        Calendar cal = Calendar.getInstance();
        cal.setTime(startRunDate);
        cal.set(Calendar.SECOND, 0);
        cal.add(Calendar.MINUTE, -1);
        startRunDate = cal.getTime();
        logger.debug("startRunDate:  " + startRunDate.toString());
        for (Artifacts.Artifact artifact : artifacts.getData()) {
            Date uploadDate = DateUtils.getDateFromUTCString(artifact.uploadDate, DateUtils.sscFormat);
            if (uploadDate == null){
                logger.debug(" uploadDate is null");
            }else{
                logger.debug(" uploadDate: " + uploadDate.toString());
            }

            if (uploadDate != null && uploadDate.after(startRunDate)) {
                theCloset = artifact;
            }
        }
        return theCloset;
    }

    /// For Unit Testing
    public SSCHandler() {
    }

    public SSCHandler(
            VulnerabilitiesQueueItem vulnerabilitiesQueueItem,
            SSCProjectConfiguration sscProjectConfiguration,
            SSCRestClient sscRestClient) {
        if (vulnerabilitiesQueueItem == null) {
            throw new IllegalArgumentException("vulnerabilities QueueItem MUST NOT be null");
        }
        if (sscProjectConfiguration == null) {
            throw new IllegalArgumentException("SSC project configuration MUST NOT be null");
        }
        if (sscRestClient == null) {
            throw new IllegalArgumentException("sscClient MUST NOT be null");
        }
        logger.debug("real initializing code");

        this.runStartTime = vulnerabilitiesQueueItem.getStartTime();

        if (!sscProjectConfiguration.isValid()) {
            throw new PermanentException("SSC configuration invalid, will not continue connecting to the server");
        } else {
            sscProjectConnector = new SSCProjectConnector(sscProjectConfiguration, sscRestClient);
            projectVersion = sscProjectConnector.getProjectVersion();
            logger.debug("Project version Id:" + projectVersion.id);
        }
    }

    public Optional<Issues> getIssuesIfScanCompleted() {
        if (!isScanProcessFinished()) {
            logger.debug("getIssuesIfScanCompleted - isScanProcessFinished = false ");
            return Optional.empty();
        }
        Issues issues = sscProjectConnector.readIssues(projectVersion.id);
        logger.debug("issues.count: "+ String.valueOf(issues.getCount()));
        return Optional.of(issues);
    }

    public Map<Integer, IssueDetails> getIssuesExtendedData(List<Issues.Issue> issues) {
        HashMap<Integer,IssueDetails> idToDetails = new HashMap<>();
        try {
            for (Issues.Issue issue : issues) {
                    IssueDetails issueDetails = sscProjectConnector.getIssueDetails(issue.id);
                    idToDetails.put(issue.id, issueDetails);
               
            }
        }catch (Exception e){
            logger.error("failed to add extended data for issues",e);
        }
        return idToDetails;
    }
}

