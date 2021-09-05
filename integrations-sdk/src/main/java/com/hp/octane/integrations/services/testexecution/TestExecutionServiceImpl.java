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

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.entities.EntityConstants;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
    public void executeSuiteRuns(Long workspaceId, List<Long> suiteIds, Long optionalReleaseId, String optionalSuiteRunName) throws IOException {
        for (Long suiteId : suiteIds) {
            this.validateSuiteRun(workspaceId, suiteId);
        }
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

        List<Entity> suiteRuns = this.planSuiteRuns(workspaceId, suiteIds, release, optionalSuiteRunName);
        for (Entity suiteRun : suiteRuns) {
            this.runSuiteRun(workspaceId, Long.parseLong(suiteRun.getId()));
        }
    }

    private Optional<Entity> getDefaultRelease(Long workspaceId) {
        List<Entity> entities = entitiesService.getEntities(workspaceId, EntityConstants.Release.COLLECTION_NAME, null, "-id",
                Arrays.asList(EntityConstants.Release.IS_DEFAULT_FIELD, EntityConstants.Release.NAME_FIELD));
        return entities.stream().filter(e -> e.getBooleanValue(EntityConstants.Release.IS_DEFAULT_FIELD)).findFirst();
    }

    private List<Entity> planSuiteRuns(Long workspaceId, List<Long> suiteIds, Entity release, String suiteRunName) {
        Entity status = dtoFactory.newDTO(Entity.class).setType(EntityConstants.Lists.ENTITY_NAME).setId("list_node.run_native_status.not_completed");
        String mySuiteRunName = SdkStringUtils.isEmpty(suiteRunName) ? "executed by ci" : suiteRunName;
        List<Entity> suiteRuns = suiteIds.stream().map(suiteId -> {
            Entity test = dtoFactory.newDTO(Entity.class).setType(EntityConstants.Test.ENTITY_NAME).setId(Long.toString(suiteId));
            Entity suiteRun = dtoFactory.newDTO(Entity.class)
                    .setField(EntityConstants.Run.NAME_FIELD, mySuiteRunName)
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
                + workspaceId + "/je/executors/validate-auto-suite?force=false&rerun=false&suite_run_id=&test_suite_id=" + Long.toString(suiteId);
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
        if (octaneResponse.getStatus() != 200) {
            throw new RuntimeException(octaneResponse.getBody());
        }

    }
}
