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
 *
 */
package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.IssueDetails;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Issues;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.hp.octane.integrations.services.vulnerabilities.OctaneIssueConsts.*;

public class SSCToOctaneIssueUtil {

    private final static Logger logger = LogManager.getLogger(SSCToOctaneIssueUtil.class);
    public static final String EXTERNAL_TOOL_NAME = "Fortify SSC";
    public static final String STATUS_NEW = "NEW";

    public static List<OctaneIssue> createOctaneIssues(List<Issues.Issue> issues,String remoteTag, Map<Integer, IssueDetails> issueDetailsById) {
        if (issues == null) {
            return new ArrayList<>();
        }
        logger.warn("SSCToOctane.createOctaneIssues");
        DTOFactory dtoFactory = DTOFactory.getInstance();
        List<OctaneIssue> octaneIssues = new ArrayList<>();
        for (Issues.Issue issue : issues) {
            OctaneIssue octaneIssue = createOctaneIssue(dtoFactory, issue,
                    issueDetailsById.get(issue.id));
            octaneIssues.add(octaneIssue);
            octaneIssue.setRemoteTag(remoteTag);
        }

        return octaneIssues;
    }

    private static OctaneIssue createOctaneIssue(DTOFactory dtoFactory,
                                                 Issues.Issue issue,
                                                 IssueDetails issueDetails) {
        logger.debug("enter createOctaneIssue");
        OctaneIssue octaneIssue = dtoFactory.newDTO(OctaneIssue.class);
        setOctaneAnalysis(dtoFactory, issue, octaneIssue);
        setOctaneSeverity(dtoFactory, issue, octaneIssue);
        setOctaneStatus(issue, octaneIssue);
        Map<String, String> extendedData = prepareExtendedData(issue, issueDetails);
        octaneIssue.setExtendedData(extendedData);
        octaneIssue.setPrimaryLocationFull(issue.fullFileName);
        octaneIssue.setLine(issue.lineNumber);
        octaneIssue.setRemoteId(issue.issueInstanceId);
        octaneIssue.setIntroducedDate(convertDates(issue.foundDate));
        octaneIssue.setExternalLink(issue.hRef);
        octaneIssue.setToolName(EXTERNAL_TOOL_NAME);
        octaneIssue.setCategory(issue.issueName);
        octaneIssue.setPackage(issue.package1);
        logger.debug("exit createOctaneIssue");
        return octaneIssue;
    }

    private static String convertDates(String inputFoundDate) {
        if (inputFoundDate == null) {
            return null;
        }
        //"2017-02-12T12:31:44.000+0000"
        DateFormat sourceDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date date;
        try {
            date = sourceDateFormat.parse(inputFoundDate);
            //"2018-06-03T14:06:58Z"
            SimpleDateFormat targetDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            return targetDateFormat.format(date);
        } catch (ParseException e) {
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
            return null;
        }
    }

    private static void setOctaneAnalysis(DTOFactory dtoFactory, Issues.Issue issue, OctaneIssue octaneIssue) {
//        "issueStatus" : "Unreviewed", - analysis
//        "audited" : false,- analysis
//        "reviewed" : null, - analysis ?

//        "list_node.issue_analysis_node.not_an_issue"
//        "list_node.issue_analysis_node.maybe_an_issue"
//        "list_node.issue_analysis_node.bug_submitted"
//        "list_node.issue_analysis_node.reviewed"
        String listId = null;
        if(issue.analysis != null ){
            if("Not an Issue".equals(issue.analysis) ){
                listId = NOT_AN_ISSUE;
            }else{
                    listId = MAYBE_AN_ISSUE;
            }
        }
        else if (isReviewed(issue)) {
            listId = REVIEWED;
        }
        if(listId != null) {
            octaneIssue.setAnalysis(createListNodeEntity(listId));
        }
    }

    private static boolean isReviewed(Issues.Issue issue) {
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

    private static void setOctaneStatus(Issues.Issue issue, OctaneIssue octaneIssue) {
        if (issue.scanStatus != null) {
            String listNodeId = null;
            if (issue.scanStatus.equalsIgnoreCase("UPDATED")) {
                listNodeId = ISSUE_STATE_EXISTING;
            } else if (issue.scanStatus.equalsIgnoreCase("NEW")) {
                listNodeId = ISSUE_STATE_NEW;
            } else if (issue.scanStatus.equalsIgnoreCase("REINTRODUCED")) {
                listNodeId = ISSUE_STATE_REOPEN;
            } else if (issue.scanStatus.equalsIgnoreCase("REMOVED")) {
                listNodeId = ISSUE_STATE_CLOSED;
            }
            if (isLegalOctaneState(listNodeId)) {
                octaneIssue.setState(createListNodeEntity(listNodeId));
            }
        }
    }


    private static Map<String, String> prepareExtendedData(Issues.Issue issue, IssueDetails issueDetails) {
        Map<String, String> retVal = new HashMap<>();
        retVal.put("issueName", issue.issueName);
        retVal.put("likelihood", issue.likelihood);
        retVal.put("kingdom", issue.kingdom);
        retVal.put("impact", issue.impact);
        retVal.put("confidence", issue.confidance);
        retVal.put("removedDate", issue.removedDate);
        if(issueDetails != null){
            retVal.put("summary",issueDetails.getData().brief);
            retVal.put("explanation",issueDetails.getData().detail);
            retVal.put("recommendations",issueDetails.getData().recommendation);
            retVal.put("tips",issueDetails.getData().tips);
        }
        return retVal;
    }

    private static void setOctaneSeverity(DTOFactory dtoFactory, Issues.Issue issue, OctaneIssue octaneIssue) {
        if (issue.severity != null) {
            String octaneSeverity = getOctaneSeverityFromSSCValue(issue.severity);
            octaneIssue.setSeverity(createListNodeEntity(octaneSeverity));
        }
    }

    private static String getOctaneSeverityFromSSCValue(Integer severity) {
        if (severity == null) {
            return null;
        }
        String logicalNameForSeverity = null;
        if (severity.equals(4)) {
            logicalNameForSeverity = SEVERITY_LG_NAME_CRITICAL;
        } else if (severity.equals(3)) {
            logicalNameForSeverity = SEVERITY_LG_NAME_HIGH;
        } else if (severity.equals(2)) {
            logicalNameForSeverity = SEVERITY_LG_NAME_MEDIUM;
        } else if (severity.equals(1)) {
            logicalNameForSeverity = SEVERITY_LG_NAME_LOW;
        }

        return logicalNameForSeverity;
    }

    public static Entity createListNodeEntity(String id) {
        if (id == null) {
            return null;
        }
        return DTOFactory.getInstance().newDTO(Entity.class).setType("list_node").setId(id);
    }
}
