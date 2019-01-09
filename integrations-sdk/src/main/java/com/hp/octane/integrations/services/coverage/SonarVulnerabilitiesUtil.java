package com.hp.octane.integrations.services.coverage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.dto.securityscans.impl.OctaneIssueImpl;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.coverage.dto.SonarIssue;
import com.hp.octane.integrations.services.coverage.dto.SonarRule;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.vulnerabilities.IssuesFileSerializer;
import com.hp.octane.integrations.services.vulnerabilities.SSCToOctaneIssueUtil;
import com.hp.octane.integrations.services.vulnerabilities.VulnerabilitiesQueueItem;
import com.hp.octane.integrations.services.vulnerabilities.VulnerabilitiesServiceImpl;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SonarVulnerabilitiesUtil {

    private static final String ISSUES_SEARCH_URI = "/api/issues/search";
    private static final String RULES_SEARCH_URI = "/api/rules/search";

    private String PROJECT_KEY_KEY = "PROJECT_KEY" ;
    private String SONAR_URL_KEY = "SONAR_URL";
    private String SONAR_TOKEN_KEY = "SONAR_TOKEN";
    private String REMOTE_TAG_KEY = "REMOTE_TAG";

    private static final Logger logger = LogManager.getLogger(SonarVulnerabilitiesUtil.class);
    private static final ObjectMapper mapper = new ObjectMapper();


    private String projectKey;
    private String sonarURL;
    private String sonarToken;
    private String remoteTag;

    private final OctaneSDK.SDKServicesConfigurer configurer;
    private final RestService restService;

    public SonarVulnerabilitiesUtil(VulnerabilitiesQueueItem queueItem, RestService restService,  OctaneSDK.SDKServicesConfigurer configurer ){
        Map<String,Object> additionalProperties = queueItem.getAdditionalProperties();
        this.projectKey = (String) additionalProperties.get(PROJECT_KEY_KEY );
        this.sonarURL = (String) additionalProperties.get(SONAR_URL_KEY );
        this.sonarToken = (String) additionalProperties.get(SONAR_TOKEN_KEY );
        this.remoteTag = (String) additionalProperties.get(REMOTE_TAG_KEY );
        this.configurer = configurer;
        this.restService = restService;


        if (projectKey == null || sonarURL == null || sonarToken == null || remoteTag == null){
            throw new RuntimeException("one of the following parameters can be null: PROJECT_KEY, SONAR_URL, SONAR_TOKEN, REMOTE_TAG ");
        }


    }


    public InputStream getVulnerabilitiesScanResultStream(VulnerabilitiesQueueItem queueItem) throws IOException {

        List<OctaneIssue> octaneIssues = getNonCacheVulnerabilitiesScanResultStream(queueItem);

        return IssuesFileSerializer.serializeIssues(octaneIssues);
    }


    private List<OctaneIssue> getNonCacheVulnerabilitiesScanResultStream(VulnerabilitiesQueueItem queueItem)
            throws IOException {

        List<SonarIssue> issuesFromSecurityTool = getIssuesFromSecurityTool(queueItem);

        List<String> octaneExistsIssuesIdsList = VulnerabilitiesServiceImpl.getRemoteIdsOfExistIssuesFromOctane(queueItem, this.remoteTag, restService, configurer);

        List<SonarIssue> issuesRequiredExtendedData = issuesFromSecurityTool.stream().filter(
                issue -> {
                    boolean isMissing = !octaneExistsIssuesIdsList.contains(issue.getKey());
                    return isMissing;
                }).collect(
                Collectors.toList());

        Set<String> sonarRulesKeys = issuesRequiredExtendedData.stream().map(issue -> issue.getRule()).collect(Collectors.toSet());
        Set<String>  issuesRequiredExtendedDataKeys = issuesRequiredExtendedData.stream().map(issue -> issue.getKey()).collect(Collectors.toSet());


        Map<String, SonarRule> rules = retrieveRulesFromSonar(sonarRulesKeys, queueItem);


        return packAllIssues(issuesFromSecurityTool,
                octaneExistsIssuesIdsList,
                issuesRequiredExtendedDataKeys,
                rules);
    }


    public List<SonarIssue> getIssuesFromSecurityTool(VulnerabilitiesQueueItem queueItem ) {
        StringBuilder errorMessage = new StringBuilder()
                .append("failed to get sonarqube vulnerability data for project key: ")
                .append(this.projectKey)
                .append(" with jobId: ").append(queueItem.getJobId())
                .append(" with buildId: ").append(queueItem.getBuildId());

        try {

            List<SonarIssue> sonarIssues = new ArrayList<>();

            //  retrieve coverage report from Sonar
            Integer pageIndex = 0;
            JsonNode jsonReport;
            do {
                pageIndex++;
                URIBuilder vulnerabilityQuery = createQueryForSonarVulnerability(pageIndex);
                InputStream reportStream = SonarUtils.getDataFromSonar(this.projectKey, this.sonarToken, vulnerabilityQuery);
                jsonReport = CIPluginSDKUtils.getObjectMapper().readTree(reportStream);
                sonarIssues.addAll(getSonarIssuesFromReport(jsonReport));
            } while (SonarUtils.sonarReportHasAnotherPage(pageIndex, jsonReport));
            return sonarIssues;

        } catch (Throwable throwable) {
            logger.error(errorMessage, throwable);
            throw new PermanentException(throwable);
        }
    }

    public Map<String, SonarRule> retrieveRulesFromSonar(Set<String> sonarRulesKeys, VulnerabilitiesQueueItem queueItem) {

        List<SonarRule> sonarRules = new ArrayList<>();
        try {

            //  retrieve coverage report from Sonar
            JsonNode jsonReport;

            for (String ruleKey : sonarRulesKeys) {
                URIBuilder ruleQuery = createQueryForSonarRule(this.sonarURL, ruleKey);
                InputStream reportStream = SonarUtils.getDataFromSonar(this.projectKey, this.sonarToken, ruleQuery);
                jsonReport = CIPluginSDKUtils.getObjectMapper().readTree(reportStream);
                sonarRules.add(getSonarRuleFromReport(jsonReport));
            }


        } catch (Throwable throwable) {
            logger.error("error when retrieving sonar rules ", throwable);
            throw new PermanentException(throwable);
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


    private URIBuilder createQueryForSonarVulnerability(Integer page) {
        URIBuilder uriBuilder = null;
        try {
            uriBuilder = new URIBuilder(this.sonarURL + ISSUES_SEARCH_URI);
            uriBuilder.setParameter("types", "VULNERABILITY")
                    .setParameter("componentKeys", this.projectKey)
                    .setParameter("ps", "500")
                    .setParameter("p", page.toString());

        } catch (URISyntaxException e) {
            logger.error(e);
            throw new PermanentException(e);
        }
        return uriBuilder;
    }


    private List<OctaneIssue> packAllIssues(List<SonarIssue> sonarIssues, List<String> octaneIssues,   Set<String> issuesRequiredExtendedDataKeys, Map<String, SonarRule> rules) {
        if (sonarIssues.size() == 0 && octaneIssues.size() == 0) {
            return new ArrayList<>();
        }
        List<String> remoteIssuesKeys =
                sonarIssues.stream().map(issue -> issue.getKey()).collect(Collectors.toList());

        List<String> remoteIdsToCloseInOctane = octaneIssues.stream()
                .filter(oIssue -> !remoteIssuesKeys.contains(oIssue))
                .collect(Collectors.toList());

        //Make Octane issue from remote id's.
        List<OctaneIssue> closedOctaneIssues = remoteIdsToCloseInOctane.stream()
                .map(t -> createClosedOctaneIssue(t)).collect(Collectors.toList());

        //Issues that are not closed , packed to update/create.
        List<SonarIssue> issuesToUpdate = sonarIssues.stream()
                .filter(sonarIssue -> !remoteIdsToCloseInOctane.contains(sonarIssue.getKey()))
                .collect(Collectors.toList());
        //Issues.Issue
        List<OctaneIssue> openOctaneIssues = SonarToOctaneIssueUtil.createOctaneIssues(issuesToUpdate,this.remoteTag, this.sonarURL, issuesRequiredExtendedDataKeys, rules);
        List<OctaneIssue> total = new ArrayList<>();
        total.addAll(openOctaneIssues);
        total.addAll(closedOctaneIssues);
        return total;
    }

    private OctaneIssue createClosedOctaneIssue(String remoteId) {
        Entity closedListNodeEntity = SSCToOctaneIssueUtil.createListNodeEntity("list_node.issue_state_node.closed");
        OctaneIssueImpl octaneIssue = new OctaneIssueImpl();
        octaneIssue.setRemoteId(remoteId);
        octaneIssue.setState(closedListNodeEntity);
        return octaneIssue;
    }

}
