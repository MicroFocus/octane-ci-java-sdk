package com.hp.octane.integrations.executor.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hp.octane.integrations.services.rest.RestService.ACCEPT_HEADER;
import static com.hp.octane.integrations.utils.SdkConstants.JobParameters.*;

public class MfMIAgentConverter extends TestsToRunConverter {

    private static final Logger logger = LogManager.getLogger(MfMIAgentConverter.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String RUN_ID_PARAMETER = "runId";

    private static final String MANUAL_RUN_DATA_PARAMETER = "manualRunData";

    private static final String GET_MANUAL_RUN_STEPS_URL_TEMPLATE = "/api/shared_spaces/%s/workspaces/%s/runs?fields=run_steps{actual,description,result,run,step_type,attachments,from_call_to_test,index_in_script,index_in_report},id,has_attachments,order_in_suite_run,test,run_by,name,test_name,duration,subtype,native_status,parent_suite,run_by{full_name},test{subtype}&limit=30&offset=0&order_by=order_in_suite_run,id&query=\"(parent_suite={id=%s};subtype IN 'run_manual')\"";

    private static final String GET_MANUAL_RUN_STEPS_URL_TEMPLATE_WITH_AU_TESTER_CONFIG = "/api/shared_spaces/%s/workspaces/%s/runs?fields=run_steps{actual,description,result,run,step_type,attachments,from_call_to_test,index_in_script,index_in_report},id,has_attachments,order_in_suite_run,test,run_by,name,test_name,duration,subtype,native_status,parent_suite,au_tester_configuration,run_by{full_name},test{subtype}&limit=30&offset=0&order_by=order_in_suite_run,id&query=\"(parent_suite={id=%s};subtype IN 'run_manual')\"";

    private static final String GET_MANUAL_RUN_METADATA = "/api/shared_spaces/%s/workspaces/%s/metadata/fields?query=\"entity_name='run';name='au_tester_configuration'\"";

    @Override
    protected String convertInternal(List<TestToRunData> data, String executionDirectory, Map<String, String> globalParameters) {
        ObjectNode manifest = OBJECT_MAPPER.createObjectNode();
        ArrayNode runs = OBJECT_MAPPER.createArrayNode();

        if (data != null) {
            for (TestToRunData test : data) {
                String manualRunData = test.getParameter(MANUAL_RUN_DATA_PARAMETER);
                if (SdkStringUtils.isEmpty(manualRunData)) {
                    throw new IllegalStateException("Missing MI Agent run data for test '" + test.getTestName() + "'");
                }

                try {
                    runs.add(OBJECT_MAPPER.readTree(manualRunData));
                } catch (IOException e) {
                    throw new IllegalStateException("Invalid MI Agent run data for test '" + test.getTestName() + "'", e);
                }
            }
        }

        manifest.set("data", runs);
        manifest.put("total_count", runs.size());
        try {
            return OBJECT_MAPPER.writeValueAsString(manifest);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize MI Agent execution manifest", e);
        }
    }

    @Override
    public void enrichTestsData(List<TestToRunData> tests, Map<String, String> globalParameters) {
        if (tests == null || tests.isEmpty()) {
            return;
        }

        OctaneClient octaneClient = getOctaneClient(globalParameters);
        OctaneConfiguration octaneConfig = octaneClient.getConfigurationService().getConfiguration();
        String workspaceId = getRequiredParameter(globalParameters, OCTANE_WORKSPACE_PARAMETER_NAME);
        String suiteRunId = getRequiredParameter(globalParameters, SUITE_RUN_ID_PARAMETER_NAME);

        String responseBody = fetchManualRuns(octaneClient, octaneConfig, workspaceId, suiteRunId);
        Map<String, JsonNode> runsById = indexRunsById(responseBody);

        for (TestToRunData test : tests) {
            String runId = getRequiredRunId(test);
            JsonNode runNode = runsById.get(runId);
            if (runNode == null) {
                throw new IllegalStateException("Failed to find MI Agent manual run '" + runId + "' for test '" + test.getTestName() + "'");
            }

            try {
                test.addParameters(MANUAL_RUN_DATA_PARAMETER, OBJECT_MAPPER.writeValueAsString(runNode));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to store MI Agent run data for test '" + test.getTestName() + "'", e);
            }
        }
    }

    private OctaneClient getOctaneClient(Map<String, String> globalParameters) {
        String octaneConfigId = getRequiredParameter(globalParameters, OCTANE_CONFIG_ID_PARAMETER_NAME);
        OctaneClient octaneClient = OctaneSDK.getClientByInstanceId(octaneConfigId);
        if (octaneClient == null) {
            throw new IllegalStateException("Failed to resolve Octane client for config id '" + octaneConfigId + "'");
        }
        return octaneClient;
    }

    private String fetchManualRuns(OctaneClient octaneClient, OctaneConfiguration octaneConfig, String workspaceId, String suiteRunId) {
        String sharedSpaceId = octaneConfig.getSharedSpace();
        String oldUrl = octaneConfig.getUrl() + String.format(GET_MANUAL_RUN_STEPS_URL_TEMPLATE, sharedSpaceId, workspaceId, suiteRunId);
        String newUrl = octaneConfig.getUrl() + String.format(GET_MANUAL_RUN_STEPS_URL_TEMPLATE_WITH_AU_TESTER_CONFIG, sharedSpaceId, workspaceId, suiteRunId);

        if (hasAutonomousTesterConfiguration(octaneClient, octaneConfig, workspaceId)) {
            OctaneResponse response = executeGet(octaneClient, newUrl);
            if (response != null && response.getStatus() == HttpStatus.SC_OK) {
                return response.getBody();
            }
            logger.warn("Failed to retrieve MI Agent runs with au_tester_configuration, falling back to legacy runs query. Status: {}", response != null ? response.getStatus() : "(null)");
        }

        OctaneResponse response = executeGet(octaneClient, oldUrl);
        if (response != null && response.getStatus() == HttpStatus.SC_OK) {
            return response.getBody();
        }

        throw new IllegalStateException("Failed to retrieve MI Agent manual runs from Octane. Status: " + (response != null ? response.getStatus() : "(null)"));
    }

    private boolean hasAutonomousTesterConfiguration(OctaneClient octaneClient, OctaneConfiguration octaneConfig, String workspaceId) {
        String metadataUrl = octaneConfig.getUrl() + String.format(GET_MANUAL_RUN_METADATA, octaneConfig.getSharedSpace(), workspaceId);
        OctaneResponse response = executeGet(octaneClient, metadataUrl);
        return response != null && response.getStatus() == HttpStatus.SC_OK && response.getBody() != null && response.getBody().contains("au_tester_configuration");
    }

    private OctaneResponse executeGet(OctaneClient octaneClient, String url) {
        Map<String, String> headers = new HashMap<>();
        headers.put(ACCEPT_HEADER, ContentType.APPLICATION_JSON.getMimeType());
        headers.put(OctaneRestClient.CLIENT_TYPE_HEADER, OctaneRestClient.CLIENT_TYPE_VALUE);

        OctaneRequest request = DTOFactory.getInstance()
                .newDTO(OctaneRequest.class)
                .setMethod(HttpMethod.GET)
                .setHeaders(headers)
                .setUrl(url);

        try {
            return octaneClient.getRestService().obtainOctaneRestClient().execute(request);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to execute Octane request: " + url, e);
        }
    }

    private Map<String, JsonNode> indexRunsById(String responseBody) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode runs = root.path("data");
            if (!runs.isArray()) {
                throw new IllegalStateException("Unexpected MI Agent runs response: missing data array");
            }

            Map<String, JsonNode> runsById = new HashMap<>();
            for (JsonNode runNode : runs) {
                String runId = runNode.path("id").asText();
                if (SdkStringUtils.isNotEmpty(runId)) {
                    runsById.put(runId, runNode);
                }
            }
            return runsById;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse MI Agent runs response", e);
        }
    }

    private String getRequiredRunId(TestToRunData test) {
        String runId = test.getParameter(RUN_ID_PARAMETER);
        if (SdkStringUtils.isEmpty(runId)) {
            throw new IllegalStateException("Missing runId parameter for MI Agent test '" + test.getTestName() + "'");
        }
        return runId;
    }

    private String getRequiredParameter(Map<String, String> globalParameters, String key) {
        if (globalParameters == null) {
            throw new IllegalStateException("Missing global parameters required for MI Agent enrichment");
        }

        String value = globalParameters.get(key);
        if (SdkStringUtils.isEmpty(value)) {
            throw new IllegalStateException("Missing global parameter '" + key + "' required for MI Agent enrichment");
        }
        return value;
    }


}
