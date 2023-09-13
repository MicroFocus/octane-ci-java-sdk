/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
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

import static com.hp.octane.integrations.services.vulnerabilities.OctaneIssueConsts.*;

public class SonarToOctaneIssueUtil {

    private final static Logger logger = LogManager.getLogger(SSCToOctaneIssueUtil.class);


    public static final String SONAR_SEVERITY_BLOCKER = "BLOCKER";
    public static final String SONAR_SEVERITY_CRITICAL = "CRITICAL";
    public static final String SONAR_SEVERITY_MAJOR = "MAJOR";
    public static final String SONAR_SEVERITY_MINOR = "MINOR";


    public static final String EXTERNAL_TOOL_NAME = "SonarQube";

    public static List<OctaneIssue> createOctaneIssues(List<SonarIssue> issues, String remoteTag, String sonarUrl, Set<String> issuesRequiredExtendedDataKeys, Map<String, SonarRule> rules) {
        if (issues == null) {
            return new ArrayList<>();
        }
        DTOFactory dtoFactory = DTOFactory.getInstance();
        List<OctaneIssue> octaneIssues = new ArrayList<>();
        for (SonarIssue issue : issues) {
            OctaneIssue octaneIssue = createOctaneIssue(dtoFactory, issue, rules, sonarUrl);
            octaneIssue.setRemoteTag(remoteTag);
            octaneIssues.add(octaneIssue);
            if (issuesRequiredExtendedDataKeys.contains(issue.getKey())) {
                Map<String, String> extendedData = prepareExtendedData(issue, rules);
                octaneIssue.setExtendedData(extendedData);
                setOctaneStatus(issue, octaneIssue, true);
            } else {
                setOctaneStatus(issue, octaneIssue, false);
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
        setPrimaryLocationFull(issue, octaneIssue);
        setExternalLink(issue, octaneIssue, sonarUrl);
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
        try {
            //convert date to string in utc
            SimpleDateFormat sourceDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssz");
            DateFormat dfUtc = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
            dfUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
            return dfUtc.format(sourceDateFormat.parse(inputFoundDate));
        } catch (ParseException e) {
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
            return null;
        }
    }


    private static void setOctaneStatus(SonarIssue issue, OctaneIssue octaneIssue, boolean isNew) {
        if (issue.getStatus() != null) {
            String listNodeId = "";

            if (issue.getStatus().equalsIgnoreCase("OPEN")) {
                listNodeId = isNew ? ISSUE_STATE_NEW : ISSUE_STATE_EXISTING;
            } else if (issue.getStatus().equalsIgnoreCase("CONFIRMED") || issue.getStatus().equalsIgnoreCase("RESOLVED")) {
                listNodeId = ISSUE_STATE_EXISTING;
            } else if (issue.getStatus().equalsIgnoreCase("REOPENED")) {
                listNodeId = ISSUE_STATE_REOPEN;
            } else if (issue.getStatus().equalsIgnoreCase("CLOSED")) {
                listNodeId = ISSUE_STATE_CLOSED;
            }
            if (isLegalOctaneState(listNodeId)) {
                octaneIssue.setState(createListNodeEntity(listNodeId));
            }
        }
    }

    private static void setPrimaryLocationFull(SonarIssue issue, OctaneIssue octaneIssue) {
        Integer offset = issue.getProject().length();
        String path = issue.getComponent().substring(offset + 1);
        octaneIssue.setPrimaryLocationFull(path);
    }

    private static void setExternalLink(SonarIssue issue, OctaneIssue octaneIssue, String sonarUrl) {
        String encodedProject = CIPluginSDKUtils.urlEncodeQueryParam(issue.getProject());
        String encodedKey = CIPluginSDKUtils.urlEncodeQueryParam(issue.getKey());
        if (!sonarUrl.substring(sonarUrl.length() - 1).equals("/")){
            sonarUrl += "/";
        }
        octaneIssue.setExternalLink(String.format("%sproject/issues?issues=%s&id=%s&open=%s", sonarUrl, encodedKey, encodedProject, encodedKey));
    }


    private static Map<String, String> prepareExtendedData(SonarIssue issue, Map<String, SonarRule> rules) {
        String ruleKey = issue.getRule();
        SonarRule rule = rules.get(ruleKey);
        Map<String, String> retVal = new HashMap<>();
        retVal.put("ruleName", rule.getName());
        if (rule.getHtmlDesc() != null) {
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
        if (severity.equals(SONAR_SEVERITY_BLOCKER) || severity.equals(SONAR_SEVERITY_CRITICAL)) {
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
