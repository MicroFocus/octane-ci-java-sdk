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
            "      \"id\": \"1042\",\n" +
            "      \"test_name\": \"Login flow\",\n" +
            "      \"subtype\": \"run_manual\",\n" +
            "      \"order_in_suite_run\": 1,\n" +
            "      \"native_status\": {\"type\": \"list_node\", \"id\": \"status_ready\", \"name\": \"Ready\"},\n" +
            "      \"test\": {\"subtype\": \"test_manual\"},\n" +
            "      \"run_steps\": {\n" +
            "        \"total_count\": 1,\n" +
            "        \"data\": [{\"id\": \"s1\", \"step_type\": {\"name\": \"Normal\"}, \"description\": \"Open login page\"}]\n" +
            "      },\n" +
            "      \"au_tester_configuration\": {\"BROWSER_NAME\": \"chrome\"}\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"run\",\n" +
            "      \"id\": \"1043\",\n" +
            "      \"test_name\": \"Checkout flow\",\n" +
            "      \"subtype\": \"run_manual\",\n" +
            "      \"order_in_suite_run\": 2,\n" +
            "      \"native_status\": {\"type\": \"list_node\", \"id\": \"status_ready\", \"name\": \"Ready\"},\n" +
            "      \"test\": {\"subtype\": \"test_manual\"},\n" +
            "      \"run_steps\": {\n" +
            "        \"total_count\": 2,\n" +
            "        \"data\": [\n" +
            "          {\"id\": \"s2\", \"step_type\": {\"name\": \"Normal\"}, \"description\": \"Add item\"},\n" +
            "          {\"id\": \"s3\", \"step_type\": {\"name\": \"Validate\"}, \"description\": \"Verify total\"}\n" +
            "        ]\n" +
            "      }\n" +
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
        Assert.assertTrue(run1.has("run_steps"));
        Assert.assertEquals("chrome", run1.path("au_tester_configuration").path("BROWSER_NAME").asText());

        JsonNode run2 = MAPPER.readTree(test2.getParameter("manualRunData"));
        Assert.assertEquals("1043", run2.path("id").asText());
        Assert.assertEquals(2, run2.path("run_steps").path("data").size());
    }

    @Test
    public void enrichTestsData_fallbackWhenNoAuConfig() throws Exception {
        TestToRunData test = new TestToRunData().setTestName("T1").addParameters("runId", "1042");
        String legacyRuns = "{\"data\":[{\"type\":\"run\",\"id\":\"1042\",\"test_name\":\"T1\",\"run_steps\":{\"data\":[],\"total_count\":0}}],\"total_count\":1}";

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

        IllegalStateException ex = Assert.assertThrows(
                IllegalStateException.class,
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
