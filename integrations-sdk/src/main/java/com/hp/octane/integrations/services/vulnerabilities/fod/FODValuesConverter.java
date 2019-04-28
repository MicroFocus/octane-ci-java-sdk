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
package com.hp.octane.integrations.services.vulnerabilities.fod;


import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.dto.securityscans.impl.OctaneIssueImpl;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.FODConstants;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.FodConnectionFactory;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.pojos.FODUser;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.pojos.Vulnerability;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.pojos.VulnerabilityAllData;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.services.FODUsersRestService;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.hp.octane.integrations.services.vulnerabilities.OctaneIssueConsts.*;
import static com.hp.octane.integrations.services.vulnerabilities.ssc.SSCToOctaneIssueUtil.createListNodeEntity;

/**
 * Created by hijaziy on 11/6/2017.
 */
public class FODValuesConverter {
    private static final Logger logger = LogManager.getLogger(FODValuesConverter.class);
    private List<FODUser> allUsers;

    public void init() {
        allUsers = FODUsersRestService.getAllUsers();
    }

    private OctaneIssue createIssue(Vulnerability vulnerability,
                                    String remoteTag,
                                    VulnerabilityAllData vulnerabilityAllData,
                                    Date baselineDate) {
        OctaneIssue entity = new OctaneIssueImpl();

        entity.setCategory(vulnerability.category);

        if (vulnerability.introducedDate != null) {
            String dateAsString = getIntroducedDate(vulnerability.introducedDate, baselineDate);
            entity.setIntroducedDate(dateAsString);
        }
        setAdditionalData(vulnerability, entity, vulnerabilityAllData);
        entity.setLine(vulnerability.lineNumber);
        entity.setToolName(FODConstants.FODTool);
        entity.setPackage(vulnerability.packageValue);
        //entity.setPRid(vulnerability.);
        entity.setPrimaryLocationFull(vulnerability.primaryLocationFull);
        setStatus(entity, vulnerability.status);
        setSeverity(entity, vulnerability.severity);
        entity.setRemoteId(vulnerability.getRemoteId());
        setExternalLink(vulnerability, entity);
        setAssignedUser(entity, vulnerability.assignedUser);
        setAnalysis(entity, vulnerability);
        entity.setRemoteTag(remoteTag);
        //TODO:
        //setToolType(entity);
        return entity;
    }

    private void setAssignedUser(OctaneIssue entity, String assignedUser) {
        String email = getEmailFromAssignedUser(assignedUser);
        if (email != null && !email.isEmpty()) {
            entity.setOwnerEmail(email);
        }
    }

    private void setExternalLink(Vulnerability vulnerability, OctaneIssue entity) {
        String entitiesURL = FodConnectionFactory.instance().getEntitiesURL();
        String externalLink = String.format("%s/releases/%s/vulnerabilities/%s/all-data", entitiesURL,
                vulnerability.releaseId,
                vulnerability.id);
        //https://api.sandbox.fortify.com/api/v3/releases/4302/vulnerabilities/217b64f9-9e73-4578-a9ea-bbe41005f858/all-data
        if (externalLink != null) {
            entity.setExternalLink(externalLink);
        }
    }

    public static boolean sameDay(Date date1, Date date2) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return fmt.format(date1).equals(fmt.format(date2));
    }

    public String getIntroducedDate(String fodIntroducedDate, Date baselineDate) {

        Date date = dateOfDateString(fodIntroducedDate);
        if (date == null) {
            return null;
        }
        if (sameDay(date, baselineDate)) {
            //A minute after the baseline.
            date = new Date(baselineDate.getTime() + 1000 * 60);
        }
        SimpleDateFormat targetDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String retVal = targetDateFormat.format(date);
        logger.debug("FOD Issue with introduced date:" + retVal);
        return retVal;
    }

    public static Date dateOfDateString(String fodIntroducedDate) {
        DateFormat sourceDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        try {
            return sourceDateFormat.parse(fodIntroducedDate + "T00:00:00");
        } catch (ParseException e) {
            return null;
        }
    }

    private void setAnalysis(OctaneIssue entity, Vulnerability vulnerability) {
        if (isReviewed(vulnerability)) {
            Entity analysisListNode = createListNodeEntity(
                    "list_node.issue_analysis_node.reviewed");
            entity.setAnalysis(analysisListNode);
        } else {
            String listNodeId = mapFODAnalysisToLogicalName(vulnerability.status);
            if (listNodeId == null) {
                listNodeId = mapAuditorStatusToAnalysis(vulnerability.auditorStatus);
            }
            if (listNodeId != null) {
                Entity analysisListNode = createListNodeEntity(listNodeId);
                entity.setAnalysis(analysisListNode);
            }
        }
    }

    private static boolean isReviewed(Vulnerability issue) {
        boolean returnValue = false;
        if (issue.status != null && issue.status.equalsIgnoreCase("reviewed")) {
            returnValue = true;
        } else if (issue.reviewed != null && issue.reviewed) {
            returnValue = true;
        } else if (issue.audited != null && issue.audited) {
            returnValue = true;
        }
        return returnValue;
    }

    public List<OctaneIssue> createOctaneIssuesFromVulns(List<Vulnerability> newVulnerabilities,
                                                         String remoteTag,
                                                         Map<String, VulnerabilityAllData> idToAllData,
                                                         Date baselineDate) {
        if (newVulnerabilities.size() == 0) {
            return new ArrayList<>();
        }
        List<OctaneIssue> issuesToCreate = new ArrayList<>();
        for (Vulnerability vulnerability : newVulnerabilities) {
            issuesToCreate.add(
                    createIssue(vulnerability, remoteTag, idToAllData.get(vulnerability.id), baselineDate));
        }
        return issuesToCreate;
    }

    private void setAdditionalData(Vulnerability vulnerability, OctaneIssue entity, VulnerabilityAllData vulnerabilityAllData) {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put(FODConstants.kingdom, vulnerability.kingdom);
        additionalData.put(FODConstants.subtype, vulnerability.subtype);
        if (vulnerabilityAllData != null) {
            if (vulnerabilityAllData.details != null) {
                additionalData.put("summary", vulnerabilityAllData.details.summary);
                additionalData.put("explanation", vulnerabilityAllData.details.explanation);
            }
            if (vulnerabilityAllData.recommendations != null) {
                additionalData.put("recommendations", vulnerabilityAllData.recommendations.recommendations);
                additionalData.put("tips", vulnerabilityAllData.recommendations.tips);
            }
        }
        entity.setExtendedData(additionalData);
    }

    private void setStatus(OctaneIssue entity, String status) {
        String logicalName = mapFODStatusToLogicalName(status);
        if (logicalName != null) {
            Entity stateListNode = createListNodeEntity(logicalName);
            entity.setState(stateListNode);
        }
    }

    private String mapFODStatusToLogicalName(String status) {
        switch (status) {
            case "New":
                return ISSUE_STATE_NEW;
            case "Existing":
                return ISSUE_STATE_EXISTING;
            case "close":
                return ISSUE_STATE_CLOSED;
            default:
                return null;
        }
    }

    private String mapFODAnalysisToLogicalName(String analysis) {
        switch (analysis) {
            case "Waiting for review":
                return MAYBE_AN_ISSUE;
            case "Reviewed":
                return REVIEWED;
            case "bug submitted":
                return BUG_SUBMITTED;
            case "Not an issue":
                return NOT_AN_ISSUE;
            default:
                return null;
        }
    }

    private String mapAuditorStatusToAnalysis(String auditorStatus) {
        String returnValue = null;
        if ("Pending Review".equalsIgnoreCase(auditorStatus)) {
            returnValue = MAYBE_AN_ISSUE;
        } else if ("Not an Issue".equalsIgnoreCase(auditorStatus) ||
                "Risk accepted".equalsIgnoreCase(auditorStatus)) {
            returnValue = NOT_AN_ISSUE;
        } else if ("Remediation Required".equalsIgnoreCase(auditorStatus) ||
                "Remediation Deferred".equalsIgnoreCase(auditorStatus) ||
                "Risk Mitigated".equalsIgnoreCase(auditorStatus)) {
            returnValue = IS_AN_ISSUE;
        }
        return returnValue;
    }

    private void setSeverity(OctaneIssue entity, Integer severity) {
        if (severity == null) {
            return;
        }
        String logicalNameForSeverity = null;
        if (severity.equals(4)) {
            logicalNameForSeverity = SEVERITY_LG_NAME_CRITICAL;
        }
        else if (severity.equals(3)) {
            logicalNameForSeverity = SEVERITY_LG_NAME_HIGH;
        }
        else if (severity.equals(2)) {
            logicalNameForSeverity = SEVERITY_LG_NAME_MEDIUM;
        }
        else if (severity.equals(1)) {
            logicalNameForSeverity = SEVERITY_LG_NAME_LOW;
        }

        Entity listNodeEntity = createListNodeEntity(logicalNameForSeverity);
        if (listNodeEntity != null) {
            entity.setSeverity(listNodeEntity);
        }
    }

    private String getEmailFromAssignedUser(String assignedUser) {
        if (SdkStringUtils.isNotEmpty(assignedUser)) {
            Optional<FODUser> foundOptional = allUsers.stream()
                    .filter(user -> assignedUser.equals(user.userName) || assignedUser.equals(getFullName(user)))
                    .findFirst();
            if (foundOptional.isPresent()) {
                return foundOptional.get().email;
            }
        }

        return null;
    }

    private String getFullName(FODUser user) {
        //"assignedUser": "Hijazi, Yamin",
        return String.format("%s, %s", user.lastName, user.firstName);
    }
}
