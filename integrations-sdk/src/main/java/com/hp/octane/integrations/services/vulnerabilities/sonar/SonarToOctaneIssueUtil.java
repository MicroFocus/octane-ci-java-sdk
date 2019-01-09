package com.hp.octane.integrations.services.vulnerabilities.sonar;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.services.vulnerabilities.sonar.dto.SonarIssue;
import com.hp.octane.integrations.services.vulnerabilities.sonar.dto.SonarRule;
import com.hp.octane.integrations.services.vulnerabilities.ssc.SSCToOctaneIssueUtil;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SonarToOctaneIssueUtil {

    private final static Logger logger = LogManager.getLogger(SSCToOctaneIssueUtil.class);
    public static final String SEVERITY_LG_NAME_LOW = "list_node.severity.low";
    public static final String SEVERITY_LG_NAME_MEDIUM = "list_node.severity.medium";
    public static final String SEVERITY_LG_NAME_HIGH = "list_node.severity.high";
    public static final String SEVERITY_LG_NAME_CRITICAL = "list_node.severity.urgent";

    public static final String SONAR_SEVERITY_BLOCKER = "BLOCKER";
    public static final String SONAR_SEVERITY_CRITICAL = "CRITICAL";
    public static final String SONAR_SEVERITY_MAJOR = "MAJOR";
    public static final String SONAR_SEVERITY_MINOR = "MINOR";




    public static final String EXTERNAL_TOOL_NAME = "SonarQube";

    public static List<OctaneIssue> createOctaneIssues(List<SonarIssue> issues, String remoteTag, String sonarUrl,  Set<String> issuesRequiredExtendedDataKeys,  Map<String, SonarRule> rules) {
        if (issues == null) {
            return new ArrayList<>();
        }
        DTOFactory dtoFactory = DTOFactory.getInstance();
        List<OctaneIssue> octaneIssues = new ArrayList<>();
        for (SonarIssue issue : issues) {
            OctaneIssue octaneIssue = createOctaneIssue(dtoFactory, issue, rules,sonarUrl);
            octaneIssue.setRemoteTag(remoteTag);
            octaneIssues.add(octaneIssue);
            if (issuesRequiredExtendedDataKeys.contains(issue.getKey())){
                Map<String, String> extendedData = prepareExtendedData(issue, rules);
                octaneIssue.setExtendedData(extendedData);
            }
        }


        return octaneIssues;
    }

    private static OctaneIssue createOctaneIssue(DTOFactory dtoFactory,
                                                 SonarIssue issue,
                                                 Map<String, SonarRule> rules,
                                                 String sonarUrl) {
        logger.debug("enter createOctaneIssue");
        OctaneIssue octaneIssue = dtoFactory.newDTO(OctaneIssue.class);
        setOctaneSeverity(issue, octaneIssue);
        setOctaneStatus(issue, octaneIssue);

        setPrimaryLocationFull(issue,octaneIssue);
        setExternalLink(issue, octaneIssue,sonarUrl);
        octaneIssue.setLine(issue.getLine());
        octaneIssue.setRemoteId(issue.getKey());
        octaneIssue.setIntroducedDate(convertDates(issue.getCreationDate()));
        octaneIssue.setToolName(EXTERNAL_TOOL_NAME);
        octaneIssue.setCategory(rules.get(issue.getRule()).getName());
        logger.debug("exit createOctaneIssue");
        return octaneIssue;
    }

    private static String convertDates(String inputFoundDate) {
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
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
            return null;
        }
    }


    private static void setOctaneStatus(SonarIssue issue, OctaneIssue octaneIssue) {
        if (issue.getStatus() != null) {
            String listNodeId = null;
            if (issue.getStatus().equalsIgnoreCase("CONFIRMED") || issue.getStatus().equalsIgnoreCase("RESOLVED")) {
                listNodeId = "list_node.issue_state_node.existing";
            } else if (issue.getStatus().equalsIgnoreCase("OPEN")) {
                listNodeId = "list_node.issue_state_node.new";
            } else if (issue.getStatus().equalsIgnoreCase("REOPENED")) {
                listNodeId = "list_node.issue_state_node.reopen";
            } else if (issue.getStatus().equalsIgnoreCase("CLOSED")) {
                listNodeId = "list_node.issue_state_node.closed";
            }
            if (isLegalOctaneState(listNodeId)) {
                octaneIssue.setState(createListNodeEntity(listNodeId));
            }
        }
    }

    private static void setPrimaryLocationFull(SonarIssue issue,OctaneIssue octaneIssue){
        Integer offset =  issue.getProject().length();
        String path =  issue.getComponent().substring(offset);
        octaneIssue.setPrimaryLocationFull(path);
    }

    private static void setExternalLink(SonarIssue issue,OctaneIssue octaneIssue,String sonarUrl){
        String encodedProject = CIPluginSDKUtils.urlEncodeQueryParam(issue.getProject());
        String encodedKey = CIPluginSDKUtils.urlEncodeQueryParam(issue.getKey());
        octaneIssue.setExternalLink(String.format("%sproject/issues?id=%s&open=%s",sonarUrl, encodedProject,encodedKey));
    }


    private static boolean isLegalOctaneState(String scanStatus) {
        List<String> legalNames = Arrays.asList("list_node.issue_state_node.new",
                "list_node.issue_state_node.existing",
                "list_node.issue_state_node.closed",
                "list_node.issue_state_node.reopen");
        return (legalNames.contains(scanStatus));
    }

    private static Map<String, String> prepareExtendedData(SonarIssue issue,  Map<String, SonarRule> rules) {
        String ruleKey =   issue.getRule();
        SonarRule rule = rules.get(ruleKey);
        Map<String, String> retVal = new HashMap<>();
        retVal.put("ruleName", rule.getName());
        if (rule.getHtmlDesc() != null){
            retVal.put("htmlDesc", rule.getHtmlDesc());
        }
        return retVal;
    }

    private static void setOctaneSeverity(SonarIssue issue, OctaneIssue octaneIssue) {
        if (issue.getSeverity() != null) {
            String octaneSeverity = getOctaneSeverityFromSSCValue(issue.getSeverity());
            octaneIssue.setSeverity(createListNodeEntity(octaneSeverity));
        }
    }

    private static String getOctaneSeverityFromSSCValue(String severity) {


        if (severity == null) {
            return null;
        }

        String logicalNameForSeverity = null;
        if (severity.equals(SONAR_SEVERITY_BLOCKER )|| severity.equals(SONAR_SEVERITY_CRITICAL)) {
            logicalNameForSeverity = SEVERITY_LG_NAME_CRITICAL;
        } else if (severity.equals(SONAR_SEVERITY_MAJOR)) {
            logicalNameForSeverity = SEVERITY_LG_NAME_HIGH;
        } else if (severity.equals(SONAR_SEVERITY_MINOR)) {
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
