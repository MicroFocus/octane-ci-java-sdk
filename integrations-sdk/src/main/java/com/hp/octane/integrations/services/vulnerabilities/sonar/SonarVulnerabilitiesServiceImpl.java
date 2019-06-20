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

package com.hp.octane.integrations.services.vulnerabilities.sonar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.dto.securityscans.impl.OctaneIssueImpl;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.queueing.QueueingService;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.sonar.SonarUtils;
import com.hp.octane.integrations.services.vulnerabilities.*;
import com.hp.octane.integrations.services.vulnerabilities.sonar.dto.SonarIssue;
import com.hp.octane.integrations.services.vulnerabilities.sonar.dto.SonarRule;
import com.hp.octane.integrations.services.vulnerabilities.ssc.SSCToOctaneIssueUtil;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hp.octane.integrations.services.vulnerabilities.OctaneIssueConsts.ISSUE_STATE_CLOSED;

public class SonarVulnerabilitiesServiceImpl implements  SonarVulnerabilitiesService {

    private static final String ISSUES_SEARCH_URI = "/api/issues/search";
    private static final String RULES_SEARCH_URI = "/api/rules/search";

    private String PROJECT_KEY_KEY = "PROJECT_KEY";
    private String SONAR_URL_KEY = "SONAR_URL";
    private String SONAR_TOKEN_KEY = "SONAR_TOKEN";
    private String REMOTE_TAG_KEY = "REMOTE_TAG";

    private static final Logger logger = LogManager.getLogger(SonarVulnerabilitiesService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    OctaneSDK.SDKServicesConfigurer configurer;
    RestService restService;

    public SonarVulnerabilitiesServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {

        if (restService == null) {
            throw new IllegalArgumentException("rest service MUST NOT be null");
        }
        if (configurer == null) {
            throw new IllegalArgumentException("configurer service MUST NOT be null");
        }
        this.restService = restService;
        this.configurer = configurer;
    }


    @Override
    public OctaneSDK.SDKServicesConfigurer getConfigurer() {
        return configurer;
    }

    @Override
    public RestService getRestService() {
        return restService;
    }

    public InputStream getVulnerabilitiesScanResultStream(VulnerabilitiesQueueItem queueItem) throws IOException{
        List<OctaneIssue> octaneIssues = getNonCacheVulnerabilitiesScanResultStream(queueItem);
        return IssuesFileSerializer.serializeIssues(octaneIssues);
    }

    @Override
    public boolean vulnerabilitiesQueueItemCleanUp(VulnerabilitiesQueueItem queueItem) {
        return true;
    }

    private List<OctaneIssue> getNonCacheVulnerabilitiesScanResultStream(VulnerabilitiesQueueItem queueItem)
            throws IOException {

        List<SonarIssue> issuesFromSecurityTool = getIssuesFromSecurityTool(queueItem);
        Set<String> sonarRulesKeys = issuesFromSecurityTool.stream().map(SonarIssue::getRule).collect(Collectors.toSet());
        Map<String, SonarRule> rules = retrieveRulesFromSonar(sonarRulesKeys, queueItem);

        List<String> octaneExistsIssuesIdsList = getRemoteIdsOfExistIssuesFromOctane(queueItem, queueItem.getAdditionalProperties().get(REMOTE_TAG_KEY));

        List<SonarIssue> issuesRequiredExtendedData = issuesFromSecurityTool.stream()
                .filter(issue -> !octaneExistsIssuesIdsList.contains(issue.getKey()))
                .collect(Collectors.toList());

        Set<String> issuesRequiredExtendedDataKeys = issuesRequiredExtendedData.stream().map(SonarIssue::getKey).collect(Collectors.toSet());

        return packAllIssues(issuesFromSecurityTool,
                octaneExistsIssuesIdsList,
                issuesRequiredExtendedDataKeys,
                rules, queueItem);
    }

    public List<SonarIssue> getIssuesFromSecurityTool(VulnerabilitiesQueueItem queueItem) {
        String projectKey = queueItem.getAdditionalProperties().get(PROJECT_KEY_KEY);
        String sonarToken = queueItem.getAdditionalProperties().get(SONAR_TOKEN_KEY);

        StringBuilder errorMessage = new StringBuilder()
                .append("failed to get sonarqube vulnerability data for project key: ")
                .append(projectKey)
                .append(" with jobId: ").append(queueItem.getJobId())
                .append(" with buildId: ").append(queueItem.getBuildId());

        try {

            List<SonarIssue> sonarIssues = new ArrayList<>();

            //  retrieve coverage report from Sonar
            Integer pageIndex = 0;
            JsonNode jsonReport;
            do {
                pageIndex++;
                URIBuilder vulnerabilityQuery = createQueryForSonarVulnerability(pageIndex, queueItem);
                InputStream reportStream = SonarUtils.getDataFromSonar(projectKey, sonarToken, vulnerabilityQuery);
                jsonReport = CIPluginSDKUtils.getObjectMapper().readTree(reportStream);
                sonarIssues.addAll(getSonarIssuesFromReport(jsonReport));
            } while (SonarUtils.sonarReportHasAnotherPage(pageIndex, jsonReport));
            return sonarIssues;

        } catch (IOException e) {
            logger.error(errorMessage, e);
            throw new PermanentException(e);
        }
    }

    public Map<String, SonarRule> retrieveRulesFromSonar(Set<String> sonarRulesKeys, VulnerabilitiesQueueItem queueItem ) throws IOException {
        String projectKey =  queueItem.getAdditionalProperties().get(PROJECT_KEY_KEY);
        String sonarURL = queueItem.getAdditionalProperties().get(SONAR_URL_KEY);
        String sonarToken = queueItem.getAdditionalProperties().get(SONAR_TOKEN_KEY);

        List<SonarRule> sonarRules = new ArrayList<>();

        //  retrieve coverage report from Sonar
        JsonNode jsonReport;

        for (String ruleKey : sonarRulesKeys) {
            URIBuilder ruleQuery = createQueryForSonarRule(sonarURL, ruleKey);
            InputStream reportStream = SonarUtils.getDataFromSonar(projectKey, sonarToken, ruleQuery);
            jsonReport = CIPluginSDKUtils.getObjectMapper().readTree(reportStream);
            sonarRules.add(getSonarRuleFromReport(jsonReport));
        }
        return sonarRules.stream().collect(Collectors.toMap(SonarRule::getKey, Function.identity()));
    }


    public SonarRule getSonarRuleFromReport(JsonNode jsonReport) {

        SonarRule sonarRule;
        JsonNode rules = jsonReport.get("rules");
        JsonNode rule = rules.get(0);

        try {
            sonarRule = mapper.treeToValue(rule, SonarRule.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sonarRule;
    }


    public List<SonarIssue> getSonarIssuesFromReport(JsonNode jsonReport) {

        List<SonarIssue> sonarIssues;
        JsonNode issues = jsonReport.get("issues");

        try {
            sonarIssues = mapper.readValue(issues.toString(), new TypeReference<List<SonarIssue>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sonarIssues;
    }

    private URIBuilder createQueryForSonarRule(String sonarURL, String sonarRuleKey) {
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(sonarURL + RULES_SEARCH_URI);
            uriBuilder.setParameter("rule_key", sonarRuleKey);

        } catch (URISyntaxException e) {
            logger.error(e);
            throw new PermanentException(e);
        }
        return uriBuilder;
    }


    private URIBuilder createQueryForSonarVulnerability(Integer page, VulnerabilitiesQueueItem queueItem) {
        String projectKey =  queueItem.getAdditionalProperties().get(PROJECT_KEY_KEY);
        String sonarURL =  queueItem.getAdditionalProperties().get(SONAR_URL_KEY);

        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(sonarURL + ISSUES_SEARCH_URI);
            uriBuilder.setParameter("types", "VULNERABILITY")
                    .setParameter("componentKeys", projectKey)
                    .setParameter("severities", "MINOR,MAJOR,CRITICAL,BLOCKER")
                    .setParameter("ps", "500")
                    .setParameter("p", page.toString());

            if (queueItem.getBaselineDate() != null) {
                uriBuilder.setParameter("createdAfter", DateUtils.convertDateToString(queueItem.getBaselineDate(), DateUtils.sonarFormat));
            }

        } catch (URISyntaxException e) {
            logger.error(e);
            throw new PermanentException(e);
        }
        return uriBuilder;
    }


    private List<OctaneIssue> packAllIssues(List<SonarIssue> sonarIssues, List<String> octaneIssues, Set<String> issuesRequiredExtendedDataKeys, Map<String, SonarRule> rules, VulnerabilitiesQueueItem queueItem) {
        String sonarURL =  queueItem.getAdditionalProperties().get(SONAR_URL_KEY);
        String remoteTag =  queueItem.getAdditionalProperties().get(REMOTE_TAG_KEY);

        if (sonarIssues.size() == 0 && octaneIssues.size() == 0) {
            return new ArrayList<>();
        }
        List<String> remoteIssuesKeys =
                sonarIssues.stream().map(SonarIssue::getKey).collect(Collectors.toList());

        List<String> remoteIdsToCloseInOctane = octaneIssues.stream()
                .filter(oIssue -> !remoteIssuesKeys.contains(oIssue))
                .collect(Collectors.toList());

        //Make Octane issue from remote id's.
        List<OctaneIssue> closedOctaneIssues = remoteIdsToCloseInOctane.stream()
                .map(this::createClosedOctaneIssue).collect(Collectors.toList());

        //Issues that are not closed , packed to update/create.
        List<SonarIssue> issuesToUpdate = sonarIssues.stream()
                .filter(sonarIssue -> !remoteIdsToCloseInOctane.contains(sonarIssue.getKey()))
                .collect(Collectors.toList());
        //Issues.Issue
        List<OctaneIssue> openOctaneIssues = SonarToOctaneIssueUtil.createOctaneIssues(issuesToUpdate,remoteTag, sonarURL, issuesRequiredExtendedDataKeys, rules);
        List<OctaneIssue> total = new ArrayList<>();
        total.addAll(openOctaneIssues);
        total.addAll(closedOctaneIssues);
        return total;
    }

    private OctaneIssue createClosedOctaneIssue(String remoteId) {
        Entity closedListNodeEntity = SSCToOctaneIssueUtil.createListNodeEntity(ISSUE_STATE_CLOSED);
        OctaneIssueImpl octaneIssue = new OctaneIssueImpl();
        octaneIssue.setRemoteId(remoteId);
        octaneIssue.setState(closedListNodeEntity);
        return octaneIssue;
    }

}
