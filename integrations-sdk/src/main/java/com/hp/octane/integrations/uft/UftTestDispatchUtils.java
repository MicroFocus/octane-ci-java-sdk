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
 *
 */


package com.hp.octane.integrations.uft;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.entities.EntityConstants;
import com.hp.octane.integrations.dto.entities.EntityList;
import com.hp.octane.integrations.dto.entities.OctaneRestExceptionData;
import com.hp.octane.integrations.exceptions.OctaneBulkException;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.entities.QueryHelper;
import com.hp.octane.integrations.uft.items.*;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class UftTestDispatchUtils {

    private final static Logger logger = LogManager.getLogger(UftTestDispatchUtils.class);

    private final static int POST_BULK_SIZE = 100;

    private final static int QUERY_CONDITION_SIZE_THRESHOLD = 3000;
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();


    public static void prepareDispatchingForFullSync(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
        matchDiscoveryTestResultsWithOctaneForFullSync(entitiesService, discoveryResult);
        matchDiscoveryDataTablesResultsWithOctaneForFullSync(entitiesService, discoveryResult);
        removeItemsWithStatusNone(discoveryResult.getAllTests());
        removeItemsWithStatusNone(discoveryResult.getAllScmResourceFiles());
    }

    public static void dispatchDiscoveryResult(EntitiesService entitiesService, UftTestDiscoveryResult result, JobRunContext jobRunContext, CustomLogger customLogger) {
        if (SdkStringUtils.isNotEmpty(result.getTestRunnerId()) && !checkExecutorExistInOctane(entitiesService, result)) {
            String msg = "Persistence [" + jobRunContext.getProjectName() + "#" + jobRunContext.getBuildNumber() + "] : executor " + result.getTestRunnerId() + " is not exist. Tests are not sent.";
            logMessage(Level.WARN, customLogger, msg);
        }

        //post new tests
        List<AutomatedTest> tests = result.getNewTests();
        if (!tests.isEmpty()) {
            boolean posted = postTests(entitiesService, tests, result.getWorkspaceId(), result.getScmRepositoryId(), result.getTestRunnerId());
            String msg = "Persistence [" + jobRunContext.getProjectName() + "#" + jobRunContext.getBuildNumber() + "] : " + tests.size() + "  new tests posted successfully = " + posted;
            logMessage(Level.INFO, customLogger, msg);
        }

        //post test updated
        tests = result.getUpdatedTests();
        if (!tests.isEmpty()) {
            boolean updated = updateTests(entitiesService, tests, result.getWorkspaceId(), result.getScmRepositoryId(), result.getTestRunnerId());
            String msg = "Persistence [" + jobRunContext.getProjectName() + "#" + jobRunContext.getBuildNumber() + "] : " + tests.size() + "  updated tests posted successfully = " + updated;
            logMessage(Level.INFO, customLogger, msg);
        }

        //post test deleted
        tests = result.getDeletedTests();
        if (!tests.isEmpty()) {
            boolean updated = updateTests(entitiesService, tests, result.getWorkspaceId(), result.getScmRepositoryId(), null);
            String msg = "Persistence [" + jobRunContext.getProjectName() + "#" + jobRunContext.getBuildNumber() + "] : " + tests.size() + "  deleted tests set as not executable successfully = " + updated;
            logMessage(Level.INFO, customLogger, msg);
        }

        //post scm resources
        List<ScmResourceFile> resources = result.getNewScmResourceFiles();
        if (!resources.isEmpty()) {
            boolean posted = postScmResources(entitiesService, resources, result.getWorkspaceId(), result.getScmRepositoryId());
            String msg = "Persistence [" + jobRunContext.getProjectName() + "#" + jobRunContext.getBuildNumber() + "] : " + resources.size() + "  new scmResources posted successfully = " + posted;
            logMessage(Level.INFO, customLogger, msg);
        }

        //update scm resources
        resources = result.getUpdatedScmResourceFiles();
        if (!resources.isEmpty()) {
            boolean posted = updateScmResources(entitiesService, resources, result.getWorkspaceId());
            String msg = "Persistence [" + jobRunContext.getProjectName() + "#" + jobRunContext.getBuildNumber() + "] : " + resources.size() + "  updated scmResources posted successfully = " + posted;
            logMessage(Level.INFO, customLogger, msg);
        }

        //delete scm resources
        resources = result.getDeletedScmResourceFiles();
        if (!resources.isEmpty()) {
            boolean posted = deleteScmResources(entitiesService, resources, result.getWorkspaceId());
            String msg = "Persistence [" + jobRunContext.getProjectName() + "#" + jobRunContext.getBuildNumber() + "] : " + resources.size() + "  scmResources deleted successfully = " + posted;
            logMessage(Level.INFO, customLogger, msg);
        }
    }

    private static boolean checkExecutorExistInOctane(EntitiesService entitiesService, UftTestDiscoveryResult result) {
        List<Entity> entities = entitiesService.getEntitiesByIds(Long.parseLong(result.getWorkspaceId()), EntityConstants.Executors.COLLECTION_NAME, Arrays.asList(result.getTestRunnerId()));
        return !entities.isEmpty();
    }

    private static void logMessage(Level level, CustomLogger customLogger, String msg) {
        logger.log(level, msg);
        if (customLogger != null) {
            try {
                customLogger.add(msg);
            } catch (Exception e) {
                logger.error("failed to add to customLogger " + e.getMessage());
            }
        }
    }

    /**
     * This method check whether discovered test are already exist on server, and instead of creation - those tests will be updated
     * Go over discovered and octane tests
     * 1.if test doesn't exist on octane - this is new test
     * 2.if test exist
     * 2.1 if test different from discovered - this is test for update
     * 2.2 if tests are equal - skip test
     * 3. all tests that are found in Octane but not discovered - those deleted tests and they will be turned to not executable
     *
     * @return true if there were changes comparing to discovered results
     */
    private static void matchDiscoveryTestResultsWithOctaneForFullSync(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
        Collection<String> additionalFields = SdkStringUtils.isNotEmpty(discoveryResult.getTestRunnerId()) ? Arrays.asList(EntityConstants.AutomatedTest.TEST_RUNNER_FIELD) : null;
        Map<String, Entity> octaneTestsMap = getTestsFromServer(entitiesService, Long.parseLong(discoveryResult.getWorkspaceId()), Long.parseLong(discoveryResult.getScmRepositoryId()), true, null, additionalFields);
        Map<String, Entity> octaneTestsMapWithoutScmRepository = getTestsFromServer(entitiesService, Long.parseLong(discoveryResult.getWorkspaceId()), Long.parseLong(discoveryResult.getScmRepositoryId()), false, null, additionalFields);


        for (AutomatedTest discoveredTest : discoveryResult.getAllTests()) {
            String key = createKey(discoveredTest.getPackage(), discoveredTest.getName());
            Entity octaneTest = octaneTestsMap.remove(key);
            Entity octaneTestWithoutScmRepository = octaneTestsMapWithoutScmRepository.remove(key);

            if (octaneTest != null) {
                //the only fields that might be different is description and executable
                boolean testsEqual = checkTestEquals(discoveredTest, octaneTest, discoveryResult.getTestRunnerId());
                if (!testsEqual) { //if equal - skip
                    discoveredTest.setId(octaneTest.getId());
                    discoveredTest.setOctaneStatus(OctaneStatus.MODIFIED);
                    if (octaneTest.containsField(EntityConstants.AutomatedTest.TEST_RUNNER_FIELD) && octaneTest.getField(EntityConstants.AutomatedTest.TEST_RUNNER_FIELD) == null) {
                        discoveredTest.setMissingTestRunner(true);
                    }
                } else {
                    discoveredTest.setOctaneStatus(OctaneStatus.NONE);
                }
            } else if (octaneTestWithoutScmRepository != null) {
                //special handling - test were injected from pipeline,or created from other fork. need to update scm repository
                discoveredTest.setId(octaneTestWithoutScmRepository.getId());
                discoveredTest.setOctaneStatus(OctaneStatus.MODIFIED);
                if (octaneTestWithoutScmRepository.containsField(EntityConstants.AutomatedTest.TEST_RUNNER_FIELD) && octaneTestWithoutScmRepository.getField(EntityConstants.AutomatedTest.TEST_RUNNER_FIELD) == null) {
                    discoveredTest.setMissingTestRunner(true);
                }
                discoveredTest.setMissingScmRepository(true);
            }
            //else do nothing, status of test should remain NEW
        }

        //go over executable tests that exist in Octane but not discovered and disable them
        for (Entity octaneTest : octaneTestsMap.values()) {
            boolean octaneExecutable = octaneTest.getBooleanValue(EntityConstants.AutomatedTest.EXECUTABLE_FIELD);
            if (octaneExecutable) {
                AutomatedTest test = new AutomatedTest();
                discoveryResult.getAllTests().add(test);
                test.setId(octaneTest.getId());
                test.setExecutable(false);
                test.setName(octaneTest.getName());
                test.setPackage(octaneTest.getStringValue(EntityConstants.AutomatedTest.PACKAGE_FIELD));
                test.setOctaneStatus(OctaneStatus.DELETED);
            }
        }
    }

    public static boolean checkTestEquals(AutomatedTest discoveredTest, Entity octaneTest, String testRunnerId) {
        boolean octaneExecutable = octaneTest.getBooleanValue(EntityConstants.AutomatedTest.EXECUTABLE_FIELD);
        String octaneDesc = octaneTest.getStringValue(EntityConstants.AutomatedTest.DESCRIPTION_FIELD);
        octaneDesc = (SdkStringUtils.isEmpty(octaneDesc) || "null".equals(octaneDesc)) ? "" : octaneDesc;
        String discoveredDesc = SdkStringUtils.isEmpty(discoveredTest.getDescription()) ? "" : discoveredTest.getDescription();
        boolean descriptionEquals = (SdkStringUtils.isEmpty(octaneDesc) && SdkStringUtils.isEmpty(discoveredDesc)) || octaneDesc.contains(discoveredDesc);
        boolean testRunnerMissing = (SdkStringUtils.isNotEmpty(testRunnerId) && octaneTest.getField(EntityConstants.AutomatedTest.TEST_RUNNER_FIELD) == null);

        return (octaneExecutable && descriptionEquals && !discoveredTest.getIsMoved() && !testRunnerMissing);
    }

    /**
     * Go over discovered and octane data tables
     * 1.if DT doesn't exist on octane - this is new DT
     * 2. all DTs that are found in Octane but not discovered - delete those DTs from server
     */
    private static boolean matchDiscoveryDataTablesResultsWithOctaneForFullSync(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
        boolean hasDiff = false;


        Map<String, Entity> octaneDataTablesMap = getDataTablesFromServer(entitiesService, Long.parseLong(discoveryResult.getWorkspaceId()), Long.parseLong(discoveryResult.getScmRepositoryId()), null);
        for (ScmResourceFile dataTable : discoveryResult.getAllScmResourceFiles()) {
            Entity octaneDataTable = octaneDataTablesMap.remove(dataTable.getRelativePath());
            if (octaneDataTable != null) {//found in Octnat - skip
                dataTable.setOctaneStatus(OctaneStatus.NONE);
                hasDiff = true;
            }
        }

        //go over DT that exist in Octane but not discovered
        for (Entity octaneDataTable : octaneDataTablesMap.values()) {
            hasDiff = true;
            ScmResourceFile dt = new ScmResourceFile();
            dt.setId(octaneDataTable.getId());
            dt.setName(octaneDataTable.getName());
            dt.setRelativePath(octaneDataTable.getStringValue(EntityConstants.ScmResourceFile.RELATIVE_PATH_FIELD));
            dt.setOctaneStatus(OctaneStatus.DELETED);
            discoveryResult.getAllScmResourceFiles().add(dt);
        }

        return hasDiff;
    }

    public static Map<String, Entity> getTestsFromServer(EntitiesService entitiesService, long workspaceId, long scmRepositoryId, boolean belongToScmRepository, Collection<String> allTestNames, Collection<String> additionalFieldsToFetch) {
        List<String> conditions = new ArrayList<>();
        if (allTestNames != null && !allTestNames.isEmpty()) {
            String byNameCondition = QueryHelper.conditionIn(EntityConstants.AutomatedTest.NAME_FIELD, allTestNames, false);
            //Query string is part of UR, some servers limit request size by 4K,
            //Here we limit nameCondition by 3K, if it exceed, we will fetch all tests
            if (byNameCondition.length() < QUERY_CONDITION_SIZE_THRESHOLD) {
                conditions.add(byNameCondition);
            }
        }

        if (belongToScmRepository) {
            conditions.add(QueryHelper.conditionRef(EntityConstants.AutomatedTest.SCM_REPOSITORY_FIELD, scmRepositoryId));
        } else {
            conditions.add(QueryHelper.conditionRef(EntityConstants.AutomatedTest.TESTING_TOOL_TYPE_FIELD, "id", "list_node.testing_tool_type.uft"));
            conditions.add(QueryHelper.conditionNot(QueryHelper.conditionRef(EntityConstants.AutomatedTest.SCM_REPOSITORY_FIELD, scmRepositoryId)));
        }

        List<String> fields = new ArrayList<>(Arrays.asList(EntityConstants.AutomatedTest.ID_FIELD, EntityConstants.AutomatedTest.NAME_FIELD, EntityConstants.AutomatedTest.PACKAGE_FIELD,
                EntityConstants.AutomatedTest.EXECUTABLE_FIELD, EntityConstants.AutomatedTest.DESCRIPTION_FIELD));
        if (additionalFieldsToFetch != null && !additionalFieldsToFetch.isEmpty()) {
            fields.addAll(additionalFieldsToFetch);
        }

        List<Entity> octaneTests = entitiesService.getEntities(workspaceId, EntityConstants.AutomatedTest.COLLECTION_NAME, conditions, fields);
        Map<String, Entity> octaneTestsMapByKey = new HashMap<>();
        for (Entity octaneTest : octaneTests) {
            String key = createKey(octaneTest.getStringValue(EntityConstants.AutomatedTest.PACKAGE_FIELD), octaneTest.getName());
            octaneTestsMapByKey.put(key, octaneTest);
        }
        return octaneTestsMapByKey;
    }

    public static Map<String, Entity> getDataTablesFromServer(EntitiesService entitiesService, long workspaceId, long scmRepositoryId, Set<String> allNames) {
        List<String> conditions = new ArrayList<>();
        if (allNames != null && !allNames.isEmpty()) {
            String byPathCondition = QueryHelper.conditionIn(EntityConstants.ScmResourceFile.NAME_FIELD, allNames, false);

            //Query string is part of UR, some servers limit request size by 4K,
            //Here we limit nameCondition by 3K, if it exceed, we will fetch all
            if (byPathCondition.length() < QUERY_CONDITION_SIZE_THRESHOLD) {
                conditions.add(byPathCondition);
            }
        }

        String conditionByScmRepository = QueryHelper.conditionRef(EntityConstants.ScmResourceFile.SCM_REPOSITORY_FIELD, scmRepositoryId);
        conditions.add(conditionByScmRepository);

        List<String> dataTablesFields = Arrays.asList(EntityConstants.ScmResourceFile.ID_FIELD, EntityConstants.ScmResourceFile.NAME_FIELD,
                EntityConstants.ScmResourceFile.RELATIVE_PATH_FIELD);
        List<Entity> octaneDataTables = entitiesService.getEntities(workspaceId, EntityConstants.ScmResourceFile.COLLECTION_NAME, conditions, dataTablesFields);

        Map<String, Entity> octaneDataTablesMap = new HashMap<>();
        for (Entity dataTable : octaneDataTables) {
            octaneDataTablesMap.put(dataTable.getStringValue(EntityConstants.ScmResourceFile.RELATIVE_PATH_FIELD), dataTable);
        }

        return octaneDataTablesMap;
    }

    public static String createKey(String... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null || "null".equals(values[i])) {
                values[i] = "";
            }
        }
        return SdkStringUtils.join(values, "#");
    }

    private static boolean postTests(EntitiesService entitiesService, List<AutomatedTest> tests, String workspaceId, String scmRepositoryId, String testRunnerId) {

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

    private static boolean postScmResources(EntitiesService entitiesService, List<ScmResourceFile> resources, String workspaceId, String scmRepositoryId) {

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

    /**
     * Entities might be posted while they already exist in Octane, such POST request will fail with general error code will be 409.
     * The same error code might be received on other validation error.
     * In this method we check whether exist other exception than duplicate
     *
     * @param e
     * @return
     */
    private static boolean checkIfExceptionCanBeIgnoredInPOST(OctaneBulkException e, String errorPrefix) {
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

    private static boolean updateTests(EntitiesService entitiesService, Collection<AutomatedTest> tests, String workspaceId, String scmRepositoryId, String testRunnerId) {

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

    private static boolean updateScmResources(EntitiesService entitiesService, List<ScmResourceFile> updatedResourceFiles, String workspaceId) {
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

    private static boolean deleteScmResources(EntitiesService entitiesService, List<ScmResourceFile> deletedResourceFiles, String workspaceId) {
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

    public static void removeItemsWithStatusNone(List<? extends SupportsOctaneStatus> list) {
        for (int i = list.size(); i > 0; i--) {
            if (list.get(i - 1).getOctaneStatus().equals(OctaneStatus.NONE)) {
                list.remove(i - 1);
            }
        }
    }

    private static Entity createListNodeEntity(String id) {
        return dtoFactory.newDTO(Entity.class).setType("list_node").setId(id);
    }
}
