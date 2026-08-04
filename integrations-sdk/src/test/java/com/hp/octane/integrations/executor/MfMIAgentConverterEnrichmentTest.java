/*
 * Copyright 2017-2026 Open Text
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package com.hp.octane.integrations.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.executor.converters.MfMIAgentConverter;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.easymock.EasyMock.*;

public class MfMIAgentConverterEnrichmentTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String RUNS_RESPONSE = "{\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"type\": \"run\",\n" +
            "      \"workspace_id\": 2001,\n" +
            "      \"name\": \"tsMIAgent\",\n" +
            "      \"test_name\": \"Login flow\",\n" +
            "      \"order_in_suite_run\": 1,\n" +
            "      \"duration\": null,\n" +
            "      \"id\": \"1042\",\n" +
            "      \"subtype\": \"run_manual\",\n" +
            "      \"au_tester_configuration\": {\n" +
            "        \"browser\": {\"BROWSER_NAME\": \"Google Chrome\", \"BROWSER_LOCALE\": \"en-US\"},\n" +
            "        \"agent\": {\"MAX_FAILURES\": 3, \"MAX_NUMBER_OF_STEPS\": 100, \"BROWSER_USE_RUN_TIMEOUT\": 2700}\n" +
            "      },\n" +
            "      \"has_attachments\": false,\n" +
            "      \"parent_suite\": {\"type\": \"run_suite\", \"id\": \"5001\", \"name\": \"tsMIAgent\"},\n" +
            "      \"run_steps\": {\n" +
            "        \"total_count\": 1,\n" +
            "        \"data\": [\n" +
            "          {\n" +
            "            \"type\": \"run_step\",\n" +
            "            \"id\": \"s1\",\n" +
            "            \"result\": null,\n" +
            "            \"attachments\": {\"total_count\": 0, \"data\": []},\n" +
            "            \"index_in_report\": \"1\",\n" +
            "            \"index_in_script\": 0,\n" +
            "            \"description\": \"Open login page\",\n" +
            "            \"actual\": null,\n" +
            "            \"activity_level\": 0,\n" +
            "            \"from_call_to_test\": false,\n" +
            "            \"step_type\": {\"type\": \"list_node\", \"id\": \"list_node.manual_test_run_step_type.normal\", \"name\": \"Normal\"},\n" +
            "            \"run\": {\"type\": \"run_manual\", \"id\": \"1042\", \"name\": \"tsMIAgent\", \"activity_level\": 0}\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      \"test\": {\"type\": \"test_manual\", \"id\": \"9001\", \"name\": \"Login flow\", \"subtype\": \"test_manual\", \"activity_level\": 0},\n" +
            "      \"native_status\": {\"type\": \"list_node\", \"id\": \"list_node.run_native_status.not_completed\", \"name\": \"In Progress\"},\n" +
            "      \"run_by\": {\"type\": \"workspace_user\", \"id\": \"1001\", \"workspace_id\": 2001, \"full_name\": \"sa@nga\", \"activity_level\": 0}\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"run\",\n" +
            "      \"workspace_id\": 2001,\n" +
            "      \"name\": \"tsMIAgent\",\n" +
            "      \"test_name\": \"Checkout flow\",\n" +
            "      \"order_in_suite_run\": 2,\n" +
            "      \"duration\": null,\n" +
            "      \"id\": \"1043\",\n" +
            "      \"subtype\": \"run_manual\",\n" +
            "      \"has_attachments\": false,\n" +
            "      \"parent_suite\": {\"type\": \"run_suite\", \"id\": \"5001\", \"name\": \"tsMIAgent\"},\n" +
            "      \"run_steps\": {\n" +
            "        \"total_count\": 2,\n" +
            "        \"data\": [\n" +
            "          {\n" +
            "            \"type\": \"run_step\",\n" +
            "            \"id\": \"s2\",\n" +
            "            \"result\": null,\n" +
            "            \"attachments\": {\"total_count\": 0, \"data\": []},\n" +
            "            \"index_in_report\": \"1\",\n" +
            "            \"index_in_script\": 0,\n" +
            "            \"description\": \"Add item\",\n" +
            "            \"actual\": null,\n" +
            "            \"activity_level\": 0,\n" +
            "            \"from_call_to_test\": false,\n" +
            "            \"step_type\": {\"type\": \"list_node\", \"id\": \"list_node.manual_test_run_step_type.normal\", \"name\": \"Normal\"},\n" +
            "            \"run\": {\"type\": \"run_manual\", \"id\": \"1043\", \"name\": \"tsMIAgent\", \"activity_level\": 0}\n" +
            "          },\n" +
            "          {\n" +
            "            \"type\": \"run_step\",\n" +
            "            \"id\": \"s3\",\n" +
            "            \"result\": null,\n" +
            "            \"attachments\": {\"total_count\": 0, \"data\": []},\n" +
            "            \"index_in_report\": \"2\",\n" +
            "            \"index_in_script\": 1,\n" +
            "            \"description\": \"Verify total\",\n" +
            "            \"actual\": null,\n" +
            "            \"activity_level\": 0,\n" +
            "            \"from_call_to_test\": false,\n" +
            "            \"step_type\": {\"type\": \"list_node\", \"id\": \"list_node.manual_test_run_step_type.validate\", \"name\": \"Validate\"},\n" +
            "            \"run\": {\"type\": \"run_manual\", \"id\": \"1043\", \"name\": \"tsMIAgent\", \"activity_level\": 0}\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      \"test\": {\"type\": \"test_manual\", \"id\": \"9002\", \"name\": \"Checkout flow\", \"subtype\": \"test_manual\", \"activity_level\": 0},\n" +
            "      \"native_status\": {\"type\": \"list_node\", \"id\": \"list_node.run_native_status.not_completed\", \"name\": \"In Progress\"},\n" +
            "      \"run_by\": {\"type\": \"workspace_user\", \"id\": \"1001\", \"workspace_id\": 2001, \"full_name\": \"sa@nga\", \"activity_level\": 0}\n" +
            "    }\n" +
            "  ],\n" +
            "  \"total_count\": 2\n" +
            "}";

    private static final String METADATA_WITH_AU = "{\"data\":[{\"name\":\"au_tester_configuration\",\"entity_name\":\"run\"}]}";
    private static final String METADATA_WITHOUT_AU = "{\"data\":[]}";

    private OctaneClient mockClient;
    private OctaneRestClient mockRestClient;
    private RestService mockRestService;
    private ConfigurationService mockConfigService;
    private OctaneConfiguration realConfig;
    private Map<OctaneConfiguration, OctaneClient> sdkClientsMap;

    @Before
    public void setUp() throws Exception {
        mockClient = createMock(OctaneClient.class);
        mockRestClient = createMock(OctaneRestClient.class);
        mockRestService = createMock(RestService.class);
        mockConfigService = createMock(ConfigurationService.class);

        // get a reference to OctaneSDK's internal client registry so we can add/remove our mock
        Field f = OctaneSDK.class.getDeclaredField("clients");
        f.setAccessible(true);
        sdkClientsMap = (Map<OctaneConfiguration, OctaneClient>) f.get(null);
    }

    @After
    public void tearDown() {
        if (realConfig != null) {
            sdkClientsMap.remove(realConfig);
        }
    }

    @Test
    public void enrichTestsData_happyPath() throws Exception {
        TestToRunData test1 = new TestToRunData().setTestName("Login flow").addParameters("runId", "1042");
        TestToRunData test2 = new TestToRunData().setTestName("Checkout flow").addParameters("runId", "1043");
        List<TestToRunData> tests = Arrays.asList(test1, test2);

        // mock Octane REST: first call = metadata check, second call = fetch runs
        realConfig = OctaneConfiguration.create("test-config", "https://octane.example.com", "1000");
        expect(mockClient.getConfigurationService()).andReturn(mockConfigService).anyTimes();
        expect(mockClient.getRestService()).andReturn(mockRestService).anyTimes();
        expect(mockConfigService.getConfiguration()).andReturn(realConfig).anyTimes();
        expect(mockRestService.obtainOctaneRestClient()).andReturn(mockRestClient).anyTimes();
        expect(mockRestClient.execute(anyObject())).andReturn(mockResponse(200, METADATA_WITH_AU));
        expect(mockRestClient.execute(anyObject())).andReturn(mockResponse(200, RUNS_RESPONSE));
        replay(mockClient, mockRestClient, mockRestService, mockConfigService);
        sdkClientsMap.put(realConfig, mockClient);

        // enrichTestsData calls Octane, fetches manual runs, attaches each run's JSON to its test
        MfMIAgentConverter converter = new MfMIAgentConverter();
        converter.enrichTestsData(tests, globalParams());

        JsonNode run1 = MAPPER.readTree(test1.getParameter("manualRunData"));
        Assert.assertEquals("1042", run1.path("id").asText());
        Assert.assertEquals("tsMIAgent", run1.path("name").asText());
        Assert.assertEquals("Login flow", run1.path("test_name").asText());
        Assert.assertEquals(1, run1.path("order_in_suite_run").asInt());
        Assert.assertEquals("run_manual", run1.path("subtype").asText());
        Assert.assertFalse(run1.path("has_attachments").asBoolean(true));
        Assert.assertEquals("5001", run1.path("parent_suite").path("id").asText());
        Assert.assertEquals("Google Chrome", run1.path("au_tester_configuration").path("browser").path("BROWSER_NAME").asText());
        Assert.assertEquals(3, run1.path("au_tester_configuration").path("agent").path("MAX_FAILURES").asInt());
        Assert.assertEquals("In Progress", run1.path("native_status").path("name").asText());
        Assert.assertEquals("sa@nga", run1.path("run_by").path("full_name").asText());
        Assert.assertTrue(run1.has("run_steps"));
        Assert.assertEquals("run_step", run1.path("run_steps").path("data").get(0).path("type").asText());
        Assert.assertEquals("Normal", run1.path("run_steps").path("data").get(0).path("step_type").path("name").asText());
        Assert.assertEquals("test_manual", run1.path("test").path("subtype").asText());

        JsonNode run2 = MAPPER.readTree(test2.getParameter("manualRunData"));
        Assert.assertEquals("1043", run2.path("id").asText());
        Assert.assertEquals("Checkout flow", run2.path("test_name").asText());
        Assert.assertEquals("5001", run2.path("parent_suite").path("id").asText());
        Assert.assertEquals(2, run2.path("run_steps").path("data").size());
        Assert.assertEquals("Validate", run2.path("run_steps").path("data").get(1).path("step_type").path("name").asText());
    }

    @Test
    public void enrichTestsData_fallbackWhenNoAuConfig() throws Exception {
        TestToRunData test = new TestToRunData().setTestName("T1").addParameters("runId", "1042");
        String legacyRuns = "{\"data\":[{\"type\":\"run\",\"workspace_id\":2001,\"name\":\"tsMIAgent\",\"id\":\"1042\",\"test_name\":\"T1\",\"subtype\":\"run_manual\",\"order_in_suite_run\":1,\"duration\":null,\"has_attachments\":false,\"parent_suite\":{\"type\":\"run_suite\",\"id\":\"5001\",\"name\":\"tsMIAgent\"},\"run_steps\":{\"total_count\":0,\"data\":[]},\"test\":{\"type\":\"test_manual\",\"id\":\"9101\",\"name\":\"T1\",\"subtype\":\"test_manual\",\"activity_level\":0},\"native_status\":{\"type\":\"list_node\",\"id\":\"list_node.run_native_status.not_completed\",\"name\":\"In Progress\"},\"run_by\":{\"type\":\"workspace_user\",\"id\":\"1001\",\"workspace_id\":2001,\"full_name\":\"sa@nga\",\"activity_level\":0}}],\"total_count\":1}";

        // mock Octane REST: metadata says no au_tester_configuration, then fetch runs with legacy URL
        realConfig = OctaneConfiguration.create("test-config", "https://octane.example.com", "1000");
        expect(mockClient.getConfigurationService()).andReturn(mockConfigService).anyTimes();
        expect(mockClient.getRestService()).andReturn(mockRestService).anyTimes();
        expect(mockConfigService.getConfiguration()).andReturn(realConfig).anyTimes();
        expect(mockRestService.obtainOctaneRestClient()).andReturn(mockRestClient).anyTimes();
        expect(mockRestClient.execute(anyObject())).andReturn(mockResponse(200, METADATA_WITHOUT_AU));
        expect(mockRestClient.execute(anyObject())).andReturn(mockResponse(200, legacyRuns));
        replay(mockClient, mockRestClient, mockRestService, mockConfigService);
        sdkClientsMap.put(realConfig, mockClient);

        MfMIAgentConverter converter = new MfMIAgentConverter();
        converter.enrichTestsData(Collections.singletonList(test), globalParams());

        JsonNode node = MAPPER.readTree(test.getParameter("manualRunData"));
        Assert.assertEquals("1042", node.path("id").asText());
        Assert.assertEquals("tsMIAgent", node.path("name").asText());
        Assert.assertEquals("run_manual", node.path("subtype").asText());
        Assert.assertEquals("5001", node.path("parent_suite").path("id").asText());
        Assert.assertEquals("sa@nga", node.path("run_by").path("full_name").asText());
        Assert.assertFalse(node.has("au_tester_configuration"));
    }

    @Test
    public void enrichTestsData_missingRunId_throws() throws Exception {
        TestToRunData test = new TestToRunData().setTestName("Bad test"); // no runId

        realConfig = OctaneConfiguration.create("test-config", "https://octane.example.com", "1000");
        expect(mockClient.getConfigurationService()).andReturn(mockConfigService).anyTimes();
        expect(mockClient.getRestService()).andReturn(mockRestService).anyTimes();
        expect(mockConfigService.getConfiguration()).andReturn(realConfig).anyTimes();
        expect(mockRestService.obtainOctaneRestClient()).andReturn(mockRestClient).anyTimes();
        expect(mockRestClient.execute(anyObject())).andReturn(mockResponse(200, METADATA_WITH_AU));
        expect(mockRestClient.execute(anyObject())).andReturn(mockResponse(200, RUNS_RESPONSE));
        replay(mockClient, mockRestClient, mockRestService, mockConfigService);
        sdkClientsMap.put(realConfig, mockClient);

        IllegalArgumentException ex = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> new MfMIAgentConverter().enrichTestsData(Collections.singletonList(test), globalParams())
        );

        Assert.assertEquals("Missing runId parameter for MI Agent test 'Bad test'", ex.getMessage());
    }

    // --- Helpers ---

    private Map<String, String> globalParams() {
        Map<String, String> params = new HashMap<>();
        params.put("octaneConfigId", "test-config");
        params.put("octaneWorkspaceId", "1001");
        params.put("suiteRunId", "5001");
        return params;
    }

    private OctaneResponse mockResponse(int status, String body) {
        OctaneResponse r = createMock(OctaneResponse.class);
        expect(r.getStatus()).andReturn(status).anyTimes();
        expect(r.getBody()).andReturn(body).anyTimes();
        replay(r);
        return r;
    }
}
