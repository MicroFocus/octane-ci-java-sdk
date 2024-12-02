/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
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
package com.hp.octane.integrations.uft;

import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.entities.EntityConstants;
import com.hp.octane.integrations.dto.entities.EntityList;
import com.hp.octane.integrations.dto.entities.OctaneRestExceptionData;
import com.hp.octane.integrations.exceptions.OctaneBulkException;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.uft.items.*;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author Itay Karo on 26/08/2021
 */
public class UftDiscoveryResultDispatcherImpl extends DiscoveryResultDispatcher {

    private static final Logger logger = LogManager.getLogger(UftDiscoveryResultDispatcherImpl.class);

    private static final int POST_BULK_SIZE = 100;

    @Override
    public void dispatchDiscoveryResults(EntitiesService entitiesService, UftTestDiscoveryResult result, JobRunContext jobRunContext, CustomLogger customLogger) {
        if (SdkStringUtils.isNotEmpty(result.getTestRunnerId()) && !checkExecutorExistInOctane(entitiesService, result)) {
            String msg = "Persistence [" + jobRunContext.getProjectName() + "#" + jobRunContext.getBuildNumber() + "] : executor " + result.getTestRunnerId() + " is not exist. Tests are not sent.";
            logMessage(logger, Level.WARN, customLogger, msg);
        }

        //post new tests
        List<AutomatedTest> tests = result.getNewTests();
        if (!tests.isEmpty()) {
            boolean posted = postTests(entitiesService, tests, result.getWorkspaceId(), result.getScmRepositoryId(), result.getTestRunnerId());
            String msg = "Persistence [" + jobRunContext.getProjectName() + "#" + jobRunContext.getBuildNumber() + "] : " + tests.size() + "  new tests posted successfully = " + posted;
            logMessage(logger, Level.INFO, customLogger, msg);
        }

        //post test updated
        tests = result.getUpdatedTests();
        if (!tests.isEmpty()) {
            boolean updated = updateTests(entitiesService, tests, result.getWorkspaceId(), result.getScmRepositoryId(), result.getTestRunnerId());
            String msg = "Persistence [" + jobRunContext.getProjectName() + "#" + jobRunContext.getBuildNumber() + "] : " + tests.size() + "  updated tests posted successfully = " + updated;
            logMessage(logger, Level.INFO, customLogger, msg);
        }

        //post test deleted
        tests = result.getDeletedTests();
        if (!tests.isEmpty()) {
            boolean updated = updateTests(entitiesService, tests, result.getWorkspaceId(), result.getScmRepositoryId(), null);
            String msg = "Persistence [" + jobRunContext.getProjectName() + "#" + jobRunContext.getBuildNumber() + "] : " + tests.size() + "  deleted tests set as not executable successfully = " + updated;
            logMessage(logger, Level.INFO, customLogger, msg);
        }

        //post scm resources
        List<ScmResourceFile> resources = result.getNewScmResourceFiles();
        if (!resources.isEmpty()) {
            boolean posted = postScmResources(entitiesService, resources, result.getWorkspaceId(), result.getScmRepositoryId());
            String msg = "Persistence [" + jobRunContext.getProjectName() + "#" + jobRunContext.getBuildNumber() + "] : " + resources.size() + "  new scmResources posted successfully = " + posted;
            logMessage(logger, Level.INFO, customLogger, msg);
        }

        //update scm resources
        resources = result.getUpdatedScmResourceFiles();
        if (!resources.isEmpty()) {
            boolean posted = updateScmResources(entitiesService, resources, result.getWorkspaceId());
            String msg = "Persistence [" + jobRunContext.getProjectName() + "#" + jobRunContext.getBuildNumber() + "] : " + resources.size() + "  updated scmResources posted successfully = " + posted;
            logMessage(logger, Level.INFO, customLogger, msg);
        }

        //delete scm resources
        resources = result.getDeletedScmResourceFiles();
        if (!resources.isEmpty()) {
            boolean posted = deleteScmResources(entitiesService, resources, result.getWorkspaceId());
            String msg = "Persistence [" + jobRunContext.getProjectName() + "#" + jobRunContext.getBuildNumber() + "] : " + resources.size() + "  scmResources deleted successfully = " + posted;
            logMessage(logger, Level.INFO, customLogger, msg);
        }

    }

    private boolean postTests(EntitiesService entitiesService, List<AutomatedTest> tests, String workspaceId, String scmRepositoryId, String testRunnerId) {

        if (!tests.isEmpty()) {
            //convert to DTO
            List<Entity> testsForPost = new ArrayList<>(tests.size());
            Entity uftTestingTool = createListNodeEntity("list_node.testing_tool_type.uft");
            Entity uftFramework = createListNodeEntity("list_node.je.framework.uft");
            Entity guiTestType = createListNodeEntity("list_node.test_type.gui");
            Entity apiTestType = createListNodeEntity("list_node.test_type.api");

            Entity scmRepository = dtoFactory.newDTO(Entity.class).setType(EntityConstants.ScmRepository.ENTITY_NAME).setId(scmRepositoryId);
            Entity testRunner = SdkStringUtils.isNotEmpty(testRunnerId) ? dtoFactory.newDTO(Entity.class).setType(EntityConstants.Executors.ENTITY_NAME).setId(testRunnerId) : null;
            for (AutomatedTest test : tests) {
                Entity testType = UftTestType.API.equals(test.getUftTestType()) ? apiTestType : guiTestType;
                EntityList testTypeList = dtoFactory.newDTO(EntityList.class).addEntity(testType);

                Entity octaneTest = dtoFactory.newDTO(Entity.class).setType(EntityConstants.AutomatedTest.ENTITY_NAME)
                        .setField(EntityConstants.AutomatedTest.TESTING_TOOL_TYPE_FIELD, uftTestingTool)
                        .setField(EntityConstants.AutomatedTest.FRAMEWORK_FIELD, uftFramework)
                        .setField(EntityConstants.AutomatedTest.TEST_TYPE_FIELD, testTypeList)
                        .setField(EntityConstants.AutomatedTest.SCM_REPOSITORY_FIELD, scmRepository)
                        .setField(EntityConstants.AutomatedTest.NAME_FIELD, test.getName())
                        .setField(EntityConstants.AutomatedTest.PACKAGE_FIELD, test.getPackage())
                        .setField(EntityConstants.AutomatedTest.DESCRIPTION_FIELD, test.getDescription())
                        .setField(EntityConstants.AutomatedTest.EXECUTABLE_FIELD, test.getExecutable());
                testsForPost.add(octaneTest);

                if (testRunner != null) {
                    octaneTest.setField(EntityConstants.AutomatedTest.TEST_RUNNER_FIELD, testRunner);
                }
            }

            //POST
            for (int i = 0; i < testsForPost.size(); i += POST_BULK_SIZE) {
                try {
                    List<Entity> subList = testsForPost.subList(i, Math.min(i + POST_BULK_SIZE, testsForPost.size()));
                    entitiesService.postEntities(Long.parseLong(workspaceId), EntityConstants.AutomatedTest.COLLECTION_NAME, subList);
                } catch (OctaneBulkException e) {
                    return checkIfExceptionCanBeIgnoredInPOST(e, "Failed to post tests");
                }
            }
        }
        return true;
    }

    private boolean postScmResources(EntitiesService entitiesService, List<ScmResourceFile> resources, String workspaceId, String scmRepositoryId) {

        if (!resources.isEmpty()) {
            //CONVERT TO DTO
            List<Entity> entitiesForPost = new ArrayList<>(resources.size());
            Entity scmRepository = dtoFactory.newDTO(Entity.class).setType(EntityConstants.ScmRepository.ENTITY_NAME).setId(scmRepositoryId);
            for (ScmResourceFile resource : resources) {
                Entity entity = dtoFactory.newDTO(Entity.class).setType(EntityConstants.ScmResourceFile.ENTITY_NAME)
                        .setName(resource.getName())
                        .setField(EntityConstants.ScmResourceFile.SCM_REPOSITORY_FIELD, scmRepository)
                        .setField(EntityConstants.ScmResourceFile.RELATIVE_PATH_FIELD, resource.getRelativePath());
                entitiesForPost.add(entity);
            }

            //POST
            for (int i = 0; i < resources.size(); i += POST_BULK_SIZE)
                try {
                    List<Entity> subList = entitiesForPost.subList(i, Math.min(i + POST_BULK_SIZE, entitiesForPost.size()));
                    entitiesService.postEntities(Long.parseLong(workspaceId), EntityConstants.ScmResourceFile.COLLECTION_NAME, subList);
                } catch (OctaneBulkException e) {
                    return checkIfExceptionCanBeIgnoredInPOST(e, "Failed to post scm resource files");
                }
        }
        return true;
    }

    private boolean updateTests(EntitiesService entitiesService, Collection<AutomatedTest> tests, String workspaceId, String scmRepositoryId, String testRunnerId) {

        Entity testRunner = SdkStringUtils.isNotEmpty(testRunnerId) ? dtoFactory.newDTO(Entity.class).setType(EntityConstants.Executors.ENTITY_NAME).setId(testRunnerId) : null;
        try {
            //CONVERT TO DTO
            List<Entity> testsForUpdate = new ArrayList<>();
            for (AutomatedTest test : tests) {
                Entity octaneTest = dtoFactory.newDTO(Entity.class)
                        .setType(EntityConstants.AutomatedTest.ENTITY_NAME)
                        .setId(test.getId())
                        .setField(EntityConstants.AutomatedTest.EXECUTABLE_FIELD, test.getExecutable());

                if (test.getDescription() != null) {
                    octaneTest.setField(EntityConstants.AutomatedTest.DESCRIPTION_FIELD, test.getDescription());
                }
                if (test.getIsMoved()) {
                    octaneTest.setName(test.getName());
                    octaneTest.setField(EntityConstants.AutomatedTest.PACKAGE_FIELD, test.getPackage());
                }
                if (test.isMissingScmRepository()) {
                    Entity scmRepository = dtoFactory.newDTO(Entity.class).setType(EntityConstants.ScmRepository.ENTITY_NAME).setId(scmRepositoryId);
                    octaneTest.setField(EntityConstants.ScmResourceFile.SCM_REPOSITORY_FIELD, scmRepository);
                }
                if (test.isMissingTestRunner() && testRunner != null) {
                    octaneTest.setField(EntityConstants.AutomatedTest.TEST_RUNNER_FIELD, testRunner);
                }
                testsForUpdate.add(octaneTest);
            }

            //PUT
            if (!testsForUpdate.isEmpty()) {
                for (int i = 0; i < tests.size(); i += POST_BULK_SIZE) {
                    List<Entity> subList = testsForUpdate.subList(i, Math.min(i + POST_BULK_SIZE, tests.size()));
                    entitiesService.updateEntities(Long.parseLong(workspaceId), EntityConstants.AutomatedTest.COLLECTION_NAME, subList);
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to update tests : " + e.getMessage());
            return false;
        }
    }

    private boolean updateScmResources(EntitiesService entitiesService, List<ScmResourceFile> updatedResourceFiles, String workspaceId) {
        try {
            //CONVERT TO DTO
            List<Entity> entitiesForUpdate = new ArrayList<>(updatedResourceFiles.size());
            for (ScmResourceFile resource : updatedResourceFiles) {
                Entity entity = dtoFactory.newDTO(Entity.class)
                        .setType(EntityConstants.ScmResourceFile.ENTITY_NAME)
                        .setName(resource.getName())
                        .setId(resource.getId())
                        .setField(EntityConstants.ScmResourceFile.RELATIVE_PATH_FIELD, resource.getRelativePath());
                entitiesForUpdate.add(entity);
            }

            if (!updatedResourceFiles.isEmpty()) {
                for (int i = 0; i < updatedResourceFiles.size(); i += POST_BULK_SIZE) {
                    List<Entity> data = entitiesForUpdate.subList(i, Math.min(i + POST_BULK_SIZE, entitiesForUpdate.size()));
                    entitiesService.updateEntities(Long.parseLong(workspaceId), EntityConstants.ScmResourceFile.COLLECTION_NAME, data);
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to update data tables : " + e.getMessage());
            return false;
        }
    }

    private boolean deleteScmResources(EntitiesService entitiesService, List<ScmResourceFile> deletedResourceFiles, String workspaceId) {
        Set<String> deletedIds = new HashSet<>();
        try {
            for (ScmResourceFile scmResource : deletedResourceFiles) {
                deletedIds.add(scmResource.getId());
            }

            entitiesService.deleteEntitiesByIds(Long.parseLong(workspaceId), EntityConstants.ScmResourceFile.COLLECTION_NAME, deletedIds);
            return true;

        } catch (Exception e) {
            logger.error("Failed to delete data tables : " + e.getMessage());
            return false;
        }
    }

    private boolean checkExecutorExistInOctane(EntitiesService entitiesService, UftTestDiscoveryResult result) {
        List<Entity> entities = entitiesService.getEntitiesByIds(Long.parseLong(result.getWorkspaceId()), EntityConstants.Executors.COLLECTION_NAME, Arrays.asList(result.getTestRunnerId()));
        return !entities.isEmpty();
    }

    /**
     * Entities might be posted while they already exist in Octane, such POST request will fail with general error code will be 409.
     * The same error code might be received on other validation error.
     * In this method we check whether exist other exception than duplicate
     *
     * @param e
     * @return
     */
    private boolean checkIfExceptionCanBeIgnoredInPOST(OctaneBulkException e, String errorPrefix) {
        boolean isRealException = true;
        if (e.getResponseStatus() == HttpStatus.SC_CONFLICT) {
            isRealException = false;
            for (OctaneRestExceptionData exceptionData : e.getData().getErrors()) {
                if (!exceptionData.getErrorCode().equals(EntityConstants.Errors.DUPLICATE_ERROR_CODE)) {
                    isRealException = true;
                }
            }
        }

        if (isRealException) {
            logger.error(errorPrefix + "  :  " + e.getMessage());
        }
        return isRealException;
    }

}
