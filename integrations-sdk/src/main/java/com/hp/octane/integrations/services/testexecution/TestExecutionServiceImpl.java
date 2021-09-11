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

package com.hp.octane.integrations.services.testexecution;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.entities.EntityConstants;
import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestToRunDataCollection;
import com.hp.octane.integrations.executor.converters.CucumberJVMConverter;
import com.hp.octane.integrations.services.SupportsConsoleLog;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.entities.QueryHelper;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of test execution service
 */

final class TestExecutionServiceImpl implements TestExecutionService {
    private static final Logger logger = LogManager.getLogger(TestExecutionServiceImpl.class);
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();

    private final OctaneSDK.SDKServicesConfigurer configurer;
    private final RestService restService;
    private final EntitiesService entitiesService;


    TestExecutionServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService, EntitiesService entitiesService) {
        if (configurer == null) {
            throw new IllegalArgumentException("invalid configurer");
        }
        if (restService == null) {
            throw new IllegalArgumentException("rest service MUST NOT be null");
        }
        if (entitiesService == null) {
            throw new IllegalArgumentException("entities service MUST NOT be null");
        }
        this.configurer = configurer;
        this.restService = restService;
        this.entitiesService = entitiesService;

        logger.info(configurer.octaneConfiguration.getLocationForLog() + "initialized SUCCESSFULLY");
    }

    @Override
    public void executeSuiteRuns(Long workspaceId, List<Long> suiteIds, Long optionalReleaseId, String optionalSuiteRunName, SupportsConsoleLog supportsConsoleLog) throws IOException {
        SupportsConsoleLog mySupportsConsoleLog = getSupportConsoleLogOrCreateEmpty(supportsConsoleLog);

        mySupportsConsoleLog.addLogMessage("Executing suite ids " + suiteIds + " in " + configurer.octaneConfiguration.getLocationForLog() + ":" + workspaceId);
        for (Long suiteId : suiteIds) {
            this.validateSuiteRun(workspaceId, suiteId);
        }

        Entity release = getReleaseOrThrow(workspaceId, optionalReleaseId);
        mySupportsConsoleLog.addLogMessage("Using release  - " + release.getId());
        List<Entity> suiteRuns = this.planSuiteRuns(workspaceId, suiteIds, release, optionalSuiteRunName);
        for (Entity suiteRun : suiteRuns) {
            this.runSuiteRun(workspaceId, Long.parseLong(suiteRun.getId()));
        }
        mySupportsConsoleLog.addLogMessage("Suite runs are started");
    }

    @Override
    public List<TestExecutionContext> prepareTestExecutionForSuites(Long workspaceId, List<Long> suiteIds, final SupportsConsoleLog supportsConsoleLog) {
        SupportsConsoleLog mySupportsConsoleLog = getSupportConsoleLogOrCreateEmpty(supportsConsoleLog);
        List<TestExecutionContext> output = new ArrayList<>();
        mySupportsConsoleLog.addLogMessage("Executing suite ids " + suiteIds + " in CI Server. Getting data from " + configurer.octaneConfiguration.getLocationForLog() + ":" + workspaceId);
        suiteIds.forEach(suiteId -> {
            List<Entity> suiteLinks = getSuiteLinks(workspaceId, suiteId);
            List<Entity> suiteLinksWithTestRunner = suiteLinks.stream().filter(e -> e.containsFieldAndValue(EntityConstants.TestSuiteLinkToTest.TEST_RUNNER_FIELD)).collect(Collectors.toList());
            int noRunnerCount = (suiteLinks.size() - suiteLinksWithTestRunner.size());
            String noRunnerMsg = noRunnerCount > 0 ? "" : String.format(", found %s test(s) without test runner, such tests will be skipped", noRunnerCount);
            mySupportsConsoleLog.addLogMessage(String.format("Suite %s: found %s test(s) %s", suiteId, suiteLinks.size(), noRunnerMsg));
            Map<String, List<Entity>> testRunnerId2links = suiteLinksWithTestRunner.stream()
                    .collect(Collectors.groupingBy(e -> ((Entity) e.getField(EntityConstants.TestSuiteLinkToTest.TEST_RUNNER_FIELD)).getId()));

            //test runners
            Map<String, Entity> id2testRunners = getTestRunners(workspaceId, testRunnerId2links.keySet()).stream().collect(Collectors.toMap(Entity::getId, Function.identity()));

            List<Entity> testRunnersFromAnotherCiServer = id2testRunners.values().stream().filter(e -> !this.configurer.octaneConfiguration.getInstanceId()
                    .equals(e.getEntityValue("ci_server").getStringValue(EntityConstants.CIServer.INSTANCE_ID_FIELD))).collect(Collectors.toList());
            if (!testRunnersFromAnotherCiServer.isEmpty()) {
                //if there are test runners from another ci server, need to remove such tests from execution
                String runnerNames = testRunnersFromAnotherCiServer.stream().map(Entity::getName).collect(Collectors.joining(","));
                mySupportsConsoleLog.addLogMessage("Found tests with test runner(s) belong to another ci server, such tests will be skipped. Test runners are : " + runnerNames);

                //remove not relevant test runners
                testRunnersFromAnotherCiServer.forEach(e -> testRunnerId2links.remove(e.getId()));
            }


            testRunnerId2links.keySet().forEach(testRunnerId -> {
                try {
                    String testsToRunJson = convertLinksToJson(testRunnerId2links.get(testRunnerId));
                    output.add(new TestExecutionContext(id2testRunners.get(testRunnerId), testsToRunJson,
                            TestExecutionIdentifierType.SUITE.SUITE, Long.toString(suiteId)));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to build testsToRun for test runner " + testRunnerId + " : " + e.getMessage());
                }
            });

        });
        return output;
    }

    private SupportsConsoleLog getSupportConsoleLogOrCreateEmpty(SupportsConsoleLog supportsConsoleLog) {
        if (supportsConsoleLog == null) {
            return msg -> {
            };
        }
        return supportsConsoleLog;
    }

    private Entity getReleaseOrThrow(Long workspaceId, Long optionalReleaseId) {
        Entity release;
        if (optionalReleaseId == null) {
            Optional<Entity> defaultRelease = this.getDefaultRelease(workspaceId);
            if (!defaultRelease.isPresent()) {
                throw new RuntimeException("Failed to find default release ");
            }
            release = defaultRelease.get();
        } else {
            release = dtoFactory.newDTO(Entity.class).setType(EntityConstants.Release.ENTITY_NAME).setId(Long.toString(optionalReleaseId));
        }
        return release;
    }

    private Map<Long, String> getSuiteNames(Long workspaceId, List<Long> suiteIds) {
        List<Entity> entities = entitiesService.getEntitiesByIds(workspaceId, EntityConstants.Test.COLLECTION_NAME, suiteIds, Collections.singletonList("name"));
        return entities.stream().collect(Collectors.toMap(e -> Long.parseLong(e.getId()), Entity::getName));
    }

    private Optional<Entity> getDefaultRelease(Long workspaceId) {
        List<Entity> entities = entitiesService.getEntities(workspaceId, EntityConstants.Release.COLLECTION_NAME, null, "-id",
                Arrays.asList(EntityConstants.Release.IS_DEFAULT_FIELD, EntityConstants.Release.NAME_FIELD));
        return entities.stream().filter(e -> e.getBooleanValue(EntityConstants.Release.IS_DEFAULT_FIELD)).findFirst();
    }

    private List<Entity> planSuiteRuns(Long workspaceId, List<Long> suiteIds, Entity release, String suiteRunName) {
        Map<Long, String> suiteNames = SdkStringUtils.isNotEmpty(suiteRunName) ? Collections.emptyMap() : this.getSuiteNames(workspaceId, suiteIds);
        Entity status = dtoFactory.newDTO(Entity.class).setType(EntityConstants.Lists.ENTITY_NAME).setId("list_node.run_native_status.not_completed");

        List<Entity> suiteRuns = suiteIds.stream().map(suiteId -> {
            Entity test = dtoFactory.newDTO(Entity.class).setType(EntityConstants.Test.ENTITY_NAME).setId(Long.toString(suiteId));
            Entity suiteRun = dtoFactory.newDTO(Entity.class)
                    .setField(EntityConstants.Run.NAME_FIELD, SdkStringUtils.isNotEmpty(suiteRunName) ? suiteRunName : suiteNames.get(suiteId))
                    .setField(EntityConstants.Run.SUBTYPE_FIELD, "run_suite")
                    .setField(EntityConstants.Run.RELEASE_FIELD, release)
                    .setField(EntityConstants.Run.TEST_FIELD, test)
                    .setField(EntityConstants.Run.NATIVE_STATUS_FIELD, status);
            return suiteRun;
        }).collect(Collectors.toList());

        List<Entity> entities = entitiesService.postEntities(workspaceId, EntityConstants.Run.COLLECTION_NAME, suiteRuns);
        return entities;
    }

    private void validateSuiteRun(Long workspaceId, Long suiteId) throws IOException {
        //https://almoctane-eur.saas.microfocus.com/internal-api/shared_spaces/274001/workspaces/1002/je/executors/validate-auto-suite?force=false&rerun=false&suite_run_id=&test_suite_id=6009

        String url = configurer.octaneConfiguration.getUrl() + RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + configurer.octaneConfiguration.getSharedSpace() + "/workspaces/"
                + workspaceId + "/je/executors/validate-auto-suite?force=false&rerun=false&suite_run_id=&test_suite_id=" + suiteId;
        OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
                .setMethod(HttpMethod.GET)
                .setUrl(url);
        OctaneResponse octaneResponse = restService.obtainOctaneRestClient().execute(request);
        if (octaneResponse.getStatus() != 200) {
            throw new RuntimeException(octaneResponse.getBody());
        }
    }

    private void runSuiteRun(Long workspaceId, Long suiteRunId) throws IOException {
        //https://admhelp.microfocus.com/octane/en/latest/Online/Content/API/Trigger_Suite_Run.htm?Highlight=Trigger%20Suite%20Run
        String url = configurer.octaneConfiguration.getUrl() + RestService.SHARED_SPACE_API_PATH_PART + configurer.octaneConfiguration.getSharedSpace() + "/workspaces/"
                + workspaceId + "/suite_runs/" + suiteRunId + "/run_auto";
        OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
                .setMethod(HttpMethod.POST)
                .setUrl(url);
        OctaneResponse octaneResponse = restService.obtainOctaneRestClient().execute(request);
        if (octaneResponse.getStatus() != 201) {
            throw new RuntimeException("runSuiteRun failed with status " + octaneResponse.getStatus() + ", message : " + octaneResponse.getBody());
        }

    }

    //*********************************** execute by suite

    private List<Entity> getSuiteLinks(Long workspaceId, Long suiteId) {
        //http://localhost:8080/dev/api/shared_spaces/1001/workspaces/1002/test_suite_link_to_tests?
        // fields=test_runner,id,order,test_id,execution_parameters,test_runner,taxonomies,run_mode,data_table,test{id,name,subtype,external_test_id,class_name,package,name,subtype,automation_identifier},
        // &order_by=order,id
        // &limit=1000
        // &query=%22(test_suite={id=1011};include_in_next_run=true;(test={(!(subtype=%27test_manual%27))}))%22
        List<String> fields = Arrays.asList("test_runner", "id", "order", "test_id", "execution_parameters", "taxonomies",
                "run_mode", "data_table{relative_path}", "test_runner",
                "test{id,name,subtype,external_test_id,class_name,package,name,subtype,automation_identifier}");
        String includeInNextCondition = QueryHelper.condition(EntityConstants.TestSuiteLinkToTest.INCLUDE_IN_NEXT_RUN_FIELD, true);
        String notManualCondition = "(test={(!(subtype='test_manual'))})";
        String suiteIdCondition = QueryHelper.conditionRef(EntityConstants.TestSuiteLinkToTest.TEST_SUITE_FIELD, suiteId);
        List<String> conditions = Arrays.asList(includeInNextCondition, notManualCondition, suiteIdCondition);
        List<Entity> entities = entitiesService.getEntities(workspaceId, EntityConstants.TestSuiteLinkToTest.COLLECTION_NAME, conditions, "order,id", fields);

        //filter our gherkin/Bdd manual tests
        List<Entity> filteredEntities = entities.stream()
                .filter(e -> e.getField("run_mode") == null ||
                        (!"list_node.run_mode.manually".equals(((Entity) e.getField("run_mode")).getId())))
                .collect(Collectors.toList());
        return filteredEntities;
    }

    private String convertLinksToJson(List<Entity> links) throws JsonProcessingException {
        TestToRunDataCollection collection = new TestToRunDataCollection();

        for (Entity link : links) {
            TestToRunData data = new TestToRunData();
            Entity test = (Entity) link.getField("test");
            data.setPackageName(test.getStringValue(EntityConstants.AutomatedTest.PACKAGE_FIELD));
            data.setClassName(test.getStringValue(EntityConstants.AutomatedTest.CLASS_NAME_FIELD));
            data.setTestName(test.getStringValue(EntityConstants.AutomatedTest.NAME_FIELD));
            if (test.containsFieldAndValue("automation_identifier")) {
                data.addParameters(CucumberJVMConverter.FEATURE_FILE_PATH, test.getStringValue("automation_identifier"));
            }
            //TODO ADD BDD SUPPORT

            if (test.containsFieldAndValue("data_table")) {
                data.addParameters(CucumberJVMConverter.FEATURE_FILE_PATH, link.getEntityValue("data_table").getStringValue("relative_path"));
            }
            if (test.containsFieldAndValue(EntityConstants.TestSuiteLinkToTest.EXECUTION_PARAMETERS_FIELD)) {
                String[] parts = link.getStringValue(EntityConstants.TestSuiteLinkToTest.EXECUTION_PARAMETERS_FIELD).split("[\n;]");
                for (String part : parts) {
                    String myPart = part.trim();
                    int splitterIndex = myPart.indexOf('=');
                    if (myPart.isEmpty() || myPart.startsWith("#") || splitterIndex == -1) {
                        continue;
                    }
                    String name = myPart.substring(0, splitterIndex).trim();
                    String value = myPart.substring(splitterIndex + 1).trim();
                    data.addParameters(name, value);
                }
            }

            collection.getTestsToRun().add(data);
        }

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper.writeValueAsString(collection);

    }

    private List<Entity> getTestRunners(Long workspaceId, Collection<String> ids) {
        //http://localhost:8080/dev/api/shared_spaces/1001/workspaces/1002/executors?fields=ci_job,ci_server{instance_id}
        List<String> fields = Arrays.asList("ci_job", "ci_server{instance_id}");
        List<Entity> entities = entitiesService.getEntitiesByIds(workspaceId, EntityConstants.Executors.COLLECTION_NAME, ids, fields);
        return entities;
    }
}
