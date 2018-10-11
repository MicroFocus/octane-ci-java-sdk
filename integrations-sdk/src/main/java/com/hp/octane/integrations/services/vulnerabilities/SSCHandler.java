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

import com.hp.octane.integrations.services.rest.SSCRestClient;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.vulnerabilities.ssc.*;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
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
	private String targetDir;
	private long runStartTime;

	public static final String SCAN_RESULT_FILE = "securityScan.json";
	public static String SEVERITY_LG_NAME_LOW = "list_node.severity.low";
	public static String SEVERITY_LG_NAME_MEDIUM = "list_node.severity.medium";
	public static String SEVERITY_LG_NAME_HIGH = "list_node.severity.high";
	public static String SEVERITY_LG_NAME_CRITICAL = "list_node.severity.urgent";
	public static String EXTERNAL_TOOL_NAME = "Fortify SSC";
	public static String ARTIFACT_STATUS_COMPLETE = "PROCESS_COMPLETE";
	public static String ARTIFACT_ERROR_PROCESSING = "ERROR_PROCESSING";

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
				artifacts.data == null) {
			return null;
		}
		Date startRunDate = new Date(this.runStartTime);

		for (Artifacts.Artifact artifact : artifacts.data) {
			Date uploadDate = SSCDateUtils.getDateFromUTCString(artifact.uploadDate, SSCDateUtils.sscFormat);
			if (uploadDate.after(startRunDate)) {
				theCloset = artifact;
			}
		}
		return theCloset;
	}

	/// For Unit Testing
	public SSCHandler() {
	}

	public SSCHandler(VulnerabilitiesServiceImpl.VulnerabilitiesQueueItem vulnerabilitiesQueueItem,
	                  String targetDir,
	                  SSCRestClient sscRestClient) {
		if (sscRestClient == null) {
			throw new PermanentException("sscClient MUST NOT be null");
		}
		if (vulnerabilitiesQueueItem == null) {
			throw new PermanentException("vulnerabilitiesQueueItem MUST NOT be null");
		}

		//"Basic QWRtaW46ZGV2c2Vjb3Bz"
		SSCFortifyConfigurations sscFortifyConfigurations = new SSCFortifyConfigurations();
		sscFortifyConfigurations.baseToken = vulnerabilitiesQueueItem.authToken;
		sscFortifyConfigurations.projectName = vulnerabilitiesQueueItem.projectName;
		sscFortifyConfigurations.projectVersion = vulnerabilitiesQueueItem.projectVersionSymbol;
		sscFortifyConfigurations.serverURL = vulnerabilitiesQueueItem.sscUrl;

		this.targetDir = targetDir;
		this.runStartTime = vulnerabilitiesQueueItem.startTime;

		if (SdkStringUtils.isEmpty(sscFortifyConfigurations.baseToken) ||
				SdkStringUtils.isEmpty(sscFortifyConfigurations.projectName) ||
				SdkStringUtils.isEmpty(sscFortifyConfigurations.projectVersion) ||
				SdkStringUtils.isEmpty(sscFortifyConfigurations.serverURL)) {
			throw new PermanentException("missing one of the SSC configuration fields (baseToken\\project\\version\\serverUrl) will not continue connecting to the server");
		} else {
			sscProjectConnector = new SscProjectConnector(sscFortifyConfigurations, sscRestClient);
			projectVersion = sscProjectConnector.getProjectVersion();
		}
	}

	public InputStream getLatestScan() {
		if (!isScanProcessFinished()) {
			return null;
		}
		logger.warn("entered getLatestScan, read issues and serialize to:" + this.targetDir);
		Issues issues = sscProjectConnector.readNewIssuesOfLastestScan(projectVersion.id);
		List<OctaneIssue> octaneIssues = createOctaneIssues(issues);
		if (octaneIssues.isEmpty()) {
			throw new PermanentException("This scan has no issues.");
		}
		IssuesFileSerializer issuesFileSerializer = new IssuesFileSerializer(targetDir, octaneIssues);
		return issuesFileSerializer.doSerializeAndCache();
	}

	public List<OctaneIssue> createOctaneIssues(Issues issues) {
		if (issues == null) {
			return new ArrayList<>();
		}
		DTOFactory dtoFactory = DTOFactory.getInstance();
		List<OctaneIssue> octaneIssues = new ArrayList<>();
		for (Issues.Issue issue : issues.data) {
			OctaneIssue octaneIssue = createOctaneIssue(dtoFactory, issue);
			octaneIssues.add(octaneIssue);
		}

		return octaneIssues;
	}

    private OctaneIssue createOctaneIssue(DTOFactory dtoFactory, Issues.Issue issue) {
        logger.debug("enter createOctaneIssue");
        OctaneIssue octaneIssue = dtoFactory.newDTO(OctaneIssue.class);
        setOctaneAnalysis(dtoFactory, issue, octaneIssue);
        setOctaneSeverity(dtoFactory, issue, octaneIssue);
        setOctaneStatus(dtoFactory, issue, octaneIssue);
        Map<String, String> extendedData = getExtendedData(issue);
        octaneIssue.setExtendedData(extendedData);
        octaneIssue.setPrimaryLocationFull(issue.fullFileName);
        octaneIssue.setLine(issue.lineNumber);
        octaneIssue.setRemoteId(issue.issueInstanceId);
        octaneIssue.setIntroducedDate(convertDates(issue.foundDate));
        octaneIssue.setExternalLink(issue.hRef);
        octaneIssue.setToolName(EXTERNAL_TOOL_NAME);
        logger.debug("exit createOctaneIssue");
        return octaneIssue;
    }

	static private String convertDates(String inputFoundDate) {
		if (inputFoundDate == null) {
			return null;
		}
		//"2017-02-12T12:31:44.000+0000"
		DateFormat sourceDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
		Date date;
		try {
			date = sourceDateFormat.parse(inputFoundDate);
			//"2018-06-03T14:06:58Z"
			SimpleDateFormat targetDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			return targetDateFormat.format(date);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void setOctaneAnalysis(DTOFactory dtoFactory, Issues.Issue issue, OctaneIssue octaneIssue) {
//        "issueStatus" : "Unreviewed", - analysis
//        "audited" : false,- analysis
//        "reviewed" : null, - analysis ?

//        "list_node.issue_analysis_node.not_an_issue"
//        "list_node.issue_analysis_node.maybe_an_issue"
//        "list_node.issue_analysis_node.bug_submitted"
//        "list_node.issue_analysis_node.reviewed"
		if (isReviewed(issue)) {
			octaneIssue.setAnalysis(createListNodeEntity(dtoFactory, "list_node.issue_analysis_node.reviewed"));
		}
	}

	private boolean isReviewed(Issues.Issue issue) {
		boolean returnValue = false;
		if (issue.issueStatus != null && issue.issueStatus.equalsIgnoreCase("reviewed")) {
			returnValue = true;
		} else if (issue.reviewed != null && issue.reviewed) {
			returnValue = true;
		} else if (issue.audited != null && issue.audited) {
			returnValue = true;
		}
		return returnValue;
	}

	private void setOctaneStatus(DTOFactory dtoFactory, Issues.Issue issue, OctaneIssue octaneIssue) {
		if (issue.scanStatus != null) {
			String listNodeId = null;
			if (issue.scanStatus.equalsIgnoreCase("UPDATED")) {
				listNodeId = "list_node.issue_state_node.existing";
			} else if (issue.scanStatus.equalsIgnoreCase("NEW")) {
				listNodeId = "list_node.issue_state_node.new";
			} else if (issue.scanStatus.equalsIgnoreCase("REINTRODUCED")) {
				listNodeId = "list_node.issue_state_node.reopen";
			} else if (issue.scanStatus.equalsIgnoreCase("REMOVED")) {
				listNodeId = "list_node.issue_state_node.closed";
			}
			if (isLegalOctaneState(listNodeId)) {
				octaneIssue.setState(createListNodeEntity(dtoFactory, listNodeId));
			}
		}
	}

	private boolean isLegalOctaneState(String scanStatus) {
		List<String> legalNames = Arrays.asList("list_node.issue_state_node.new",
				"list_node.issue_state_node.existing",
				"list_node.issue_state_node.closed",
				"list_node.issue_state_node.reopen");
		return (legalNames.contains(scanStatus));
	}

	private Map<String, String> getExtendedData(Issues.Issue issue) {
		Map<String, String> retVal = new HashMap<>();
		retVal.put("issueName", issue.issueName);
		retVal.put("likelihood", issue.likelihood);
		retVal.put("kingdom", issue.kingdom);
		retVal.put("impact", issue.impact);
		retVal.put("confidence", issue.confidance);
		retVal.put("removedDate", issue.removedDate);
		return retVal;
	}

	private void setOctaneSeverity(DTOFactory dtoFactory, Issues.Issue issue, OctaneIssue octaneIssue) {
		if (issue.severity != null) {
			String octaneSeverity = getOctaneSeverityFromSSCValue(issue.severity);
			octaneIssue.setSeverity(createListNodeEntity(dtoFactory, octaneSeverity));
		}
	}

	private String getOctaneSeverityFromSSCValue(String severity) {
		if (severity == null) {
			return null;
		}
		String logicalNameForSeverity = null;
		if (severity.startsWith("4")) {
			logicalNameForSeverity = SEVERITY_LG_NAME_CRITICAL;
		} else if (severity.startsWith("3")) {
			logicalNameForSeverity = SEVERITY_LG_NAME_HIGH;
		} else if (severity.startsWith("2")) {
			logicalNameForSeverity = SEVERITY_LG_NAME_MEDIUM;
		} else if (severity.startsWith("1")) {
			logicalNameForSeverity = SEVERITY_LG_NAME_LOW;
		}

		return logicalNameForSeverity;
	}

	private static Entity createListNodeEntity(DTOFactory dtoFactory, String id) {
		if (id == null) {
			return null;
		}
		return dtoFactory.newDTO(Entity.class).setType("list_node").setId(id);
	}
}

