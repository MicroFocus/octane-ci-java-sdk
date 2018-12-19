/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.services.rest.SSCRestClient;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.vulnerabilities.ssc.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by hijaziy on 7/23/2018.
 */

public class SSCHandler {
    private final static Logger logger = LogManager.getLogger(SSCHandler.class);
    private SscProjectConnector sscProjectConnector;
    private ProjectVersions.ProjectVersion projectVersion;
    private long runStartTime;

    public static final String SCAN_RESULT_FILE = "securityScan.json";

    public static final String ARTIFACT_STATUS_COMPLETE = "PROCESS_COMPLETE";
    public static final String ARTIFACT_ERROR_PROCESSING = "ERROR_PROCESSING";

    public boolean isScanProcessFinished() {
        logger.debug("enter isScanProcessFinished");

        Artifacts artifacts = sscProjectConnector.getArtifactsOfProjectVersion(this.projectVersion.id, 10);
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
        if (artifacts == null ||
                artifacts.getData() == null) {
            return null;
        }
        Date startRunDate = new Date(this.runStartTime);

        for (Artifacts.Artifact artifact : artifacts.getData()) {
            Date uploadDate = SSCDateUtils.getDateFromUTCString(artifact.uploadDate, SSCDateUtils.sscFormat);
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
            VulnerabilitiesServiceImpl.VulnerabilitiesQueueItem vulnerabilitiesQueueItem,
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

        this.runStartTime = vulnerabilitiesQueueItem.startTime;

        if (!sscProjectConfiguration.isValid()) {
            throw new PermanentException("SSC configuration invalid, will not continue connecting to the server");
        } else {
            sscProjectConnector = new SscProjectConnector(sscProjectConfiguration, sscRestClient);
            projectVersion = sscProjectConnector.getProjectVersion();
        }
    }

    public Optional<Issues> getIssuesIfScanCompleted() {
        if (!isScanProcessFinished()) {
            return Optional.empty();
        }
        Issues issues = sscProjectConnector.readIssues(projectVersion.id);
        return Optional.of(issues);
    }

    public Map<Integer, IssueDetails> getDetailsOfIssues(List<Issues.Issue> issues) {
        HashMap<Integer,IssueDetails> idToDetails = new HashMap<>();
        for (Issues.Issue issue : issues) {
            IssueDetails issueDetails = sscProjectConnector.getIssueDetails(issue.id);
            idToDetails.put(issue.id, issueDetails);
        }
        return idToDetails;
    }
}

