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
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.entities.QueryHelper;
import com.hp.octane.integrations.uft.items.AutomatedTest;
import com.hp.octane.integrations.uft.items.OctaneStatus;
import com.hp.octane.integrations.uft.items.ScmResourceFile;
import com.hp.octane.integrations.uft.items.UftTestDiscoveryResult;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author Itay Karo on 26/08/2021
 */
public class UftDiscoveryResultPreparerImpl implements DiscoveryResultPreparer {

    private static final Logger logger = LogManager.getLogger(UftDiscoveryResultPreparerImpl.class);

    private static final int QUERY_CONDITION_SIZE_THRESHOLD = 3000;

    private static final String OCTANE_VERSION_SUPPORTING_TEST_RENAME = "12.60.3";

    @Override
    public void prepareDiscoveryResultForDispatchInFullSyncMode(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
        matchDiscoveryTestResultsWithOctaneForFullSync(entitiesService, discoveryResult);
        matchDiscoveryDataTablesResultsWithOctaneForFullSync(entitiesService, discoveryResult);
        removeItemsWithStatusNone(discoveryResult.getAllTests());
        removeItemsWithStatusNone(discoveryResult.getAllScmResourceFiles());
    }

    @Override
    public void prepareDiscoveryResultForDispatchInScmChangesMode(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
        if (isOctaneSupportTestRename(entitiesService)) {

            if (!discoveryResult.getCombineDataTableHashCodeToTestPathListMap().isEmpty()) {
                handleMovedTestsWithBulkTestRename(discoveryResult);
            } else {
                handleMovedTests(discoveryResult);
            }

            handleMovedDataTables(discoveryResult);
        }
        validateTestDiscoveryAndCompleteTestIdsForScmChangeDetection(entitiesService, discoveryResult);
        validateTestDiscoveryAndCompleteDataTableIdsForScmChangeDetection(entitiesService, discoveryResult);

        removeItemsWithStatusNone(discoveryResult.getAllTests());
        removeItemsWithStatusNone(discoveryResult.getAllScmResourceFiles());

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
    private void matchDiscoveryTestResultsWithOctaneForFullSync(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
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

    /**
     * Go over discovered and octane data tables
     * 1.if DT doesn't exist on octane - this is new DT
     * 2. all DTs that are found in Octane but not discovered - delete those DTs from server
     */
    private boolean matchDiscoveryDataTablesResultsWithOctaneForFullSync(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
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

    private Map<String, Entity> getTestsFromServer(EntitiesService entitiesService, long workspaceId, long scmRepositoryId, boolean belongToScmRepository, Collection<String> allTestNames, Collection<String> additionalFieldsToFetch) {
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

    private Map<String, Entity> getDataTablesFromServer(EntitiesService entitiesService, long workspaceId, long scmRepositoryId, Set<String> allNames) {
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

    private String createKey(String... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null || "null".equals(values[i])) {
                values[i] = "";
            }
        }
        return SdkStringUtils.join(values, "#");
    }

    private boolean checkTestEquals(AutomatedTest discoveredTest, Entity octaneTest, String testRunnerId) {
        boolean octaneExecutable = octaneTest.getBooleanValue(EntityConstants.AutomatedTest.EXECUTABLE_FIELD);
        String octaneDesc = octaneTest.getStringValue(EntityConstants.AutomatedTest.DESCRIPTION_FIELD);
        octaneDesc = (SdkStringUtils.isEmpty(octaneDesc) || "null".equals(octaneDesc)) ? "" : octaneDesc;
        String discoveredDesc = SdkStringUtils.isEmpty(discoveredTest.getDescription()) ? "" : discoveredTest.getDescription();
        boolean descriptionEquals = (SdkStringUtils.isEmpty(octaneDesc) && SdkStringUtils.isEmpty(discoveredDesc)) || octaneDesc.contains(discoveredDesc);
        boolean testRunnerMissing = (SdkStringUtils.isNotEmpty(testRunnerId) && octaneTest.getField(EntityConstants.AutomatedTest.TEST_RUNNER_FIELD) == null);

        return (octaneExecutable && descriptionEquals && !discoveredTest.getIsMoved() && !testRunnerMissing);
    }

    private boolean isOctaneSupportTestRename(EntitiesService entitiesService) {
        try {
            String octane_version = getOctaneVersion(entitiesService);
            boolean supportTestRename = (octane_version != null && versionCompare(OCTANE_VERSION_SUPPORTING_TEST_RENAME, octane_version) <= 0);
            logger.warn("Support test rename = " + supportTestRename);
            return supportTestRename;
        } catch (Exception e) {//can occur if user doesnot have permission to get octane version
            logger.warn("Failed to check isOctaneSupportTestRename : " + e.getMessage());
            return false;
        }
    }

    private String getOctaneVersion(EntitiesService entitiesService) {
        String octaneVersion = null;

        List<Entity> entities = entitiesService.getEntities(null, "server_version", null, null);
        if (entities.size() == 1) {
            Entity entity = entities.get(0);
            octaneVersion = entity.getStringValue("version");
            logger.debug("Received Octane version - " + octaneVersion);

        } else {
            logger.error(String.format("Request for Octane version returned %s items. return version is not defined.", entities.size()));
        }

        return octaneVersion;
    }

    /**
     * Compares two version strings.
     * <p>
     * Use this instead of String.compareTo() for a non-lexicographical
     * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
     *
     * @param str1 a string of ordinal numbers separated by decimal points.
     * @param str2 a string of ordinal numbers separated by decimal points.
     * @return The result is a negative integer if str1 is _numerically_ less than str2.
     * The result is a positive integer if str1 is _numerically_ greater than str2.
     * The result is zero if the strings are _numerically_ equal.
     * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
     */
    private Integer versionCompare(String str1, String str2) {
        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");
        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }
        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        else {
            return Integer.signum(vals1.length - vals2.length);
        }
    }

    private void handleMovedTests(UftTestDiscoveryResult result) {
        List<AutomatedTest> newTests = result.getNewTests();
        List<AutomatedTest> deletedTests = result.getDeletedTests();
        if (!newTests.isEmpty() && !deletedTests.isEmpty()) {
            Map<String, AutomatedTest> dst2Test = new HashMap<>();
            Map<AutomatedTest, AutomatedTest> deleted2newMovedTests = new HashMap<>();
            for (AutomatedTest newTest : newTests) {
                if (SdkStringUtils.isNotEmpty(newTest.getChangeSetDst())) {
                    dst2Test.put(newTest.getChangeSetDst(), newTest);
                }
            }
            for (AutomatedTest deletedTest : deletedTests) {
                if (SdkStringUtils.isNotEmpty(deletedTest.getChangeSetDst()) && dst2Test.containsKey(deletedTest.getChangeSetDst())) {
                    AutomatedTest newTest = dst2Test.get(deletedTest.getChangeSetDst());
                    deleted2newMovedTests.put(deletedTest, newTest);
                }
            }

            for (Map.Entry<AutomatedTest, AutomatedTest> entry : deleted2newMovedTests.entrySet()) {
                AutomatedTest deletedTest = entry.getKey();
                AutomatedTest newTest = entry.getValue();

                newTest.setIsMoved(true);
                newTest.setOldName(deletedTest.getName());
                newTest.setOldPackage(deletedTest.getPackage());
                newTest.setOctaneStatus(OctaneStatus.MODIFIED);

                result.getAllTests().remove(deletedTest);
            }
        }
    }

    private void handleMovedTestsWithBulkTestRename(UftTestDiscoveryResult result) {
        List<AutomatedTest> newTests = result.getNewTests();
        List<AutomatedTest> deletedTests = result.getDeletedTests();
        if (!newTests.isEmpty() && !deletedTests.isEmpty()) {
            Map<String, List<AutomatedTest>> dst2Test = new HashMap<>();
            List<AbstractMap.SimpleEntry<AutomatedTest, AutomatedTest>> deleted2newMovedTests = new LinkedList<>();

            for (AutomatedTest newTest : newTests) {
                if (SdkStringUtils.isNotEmpty(newTest.getChangeSetDst())) {
                    String key = newTest.getChangeSetDst();
                    dst2Test.computeIfAbsent(key, k -> new LinkedList<>()).add(newTest);
                }
            }
            for (AutomatedTest deletedTest : deletedTests) {

                if (SdkStringUtils.isNotEmpty(deletedTest.getChangeSetDst())) {
                    String key = deletedTest.getChangeSetDst();
                    if (dst2Test.containsKey(key)) {
                        if (dst2Test.get(key).size() == 1) {
                            AutomatedTest newTest = dst2Test.get(key).get(0);
                            deleted2newMovedTests.add(new AbstractMap.SimpleEntry(deletedTest, newTest));
                        } else {
                            AbstractMap.SimpleEntry<AutomatedTest, AutomatedTest> pairsDeletedNew = createPairsDeletedNew(dst2Test.get(key), deletedTest, result);
                            if (pairsDeletedNew != null) {
                                deleted2newMovedTests.add(pairsDeletedNew);
                            } else {
                                logger.warn("since found same tests we can't determine which test modified");
                            }
                        }
                    }
                }
            }

            for (AbstractMap.SimpleEntry<AutomatedTest, AutomatedTest> entry : deleted2newMovedTests) {
                AutomatedTest deletedTest = entry.getKey();
                AutomatedTest newTest = entry.getValue();

                newTest.setIsMoved(true);
                newTest.setOldName(deletedTest.getName());
                newTest.setOldPackage(deletedTest.getPackage());
                newTest.setOctaneStatus(OctaneStatus.MODIFIED);

                result.getAllTests().remove(deletedTest);
            }
        }
    }


    private void handleMovedDataTables(UftTestDiscoveryResult result) {
        List<ScmResourceFile> newItems = result.getNewScmResourceFiles();
        List<ScmResourceFile> deletedItems = result.getDeletedScmResourceFiles();
        if (!newItems.isEmpty() && !deletedItems.isEmpty()) {
            Map<String, ScmResourceFile> dst2File = new HashMap<>();
            Map<ScmResourceFile, ScmResourceFile> deleted2newMovedFiles = new HashMap<>();
            for (ScmResourceFile newFile : newItems) {
                if (SdkStringUtils.isNotEmpty(newFile.getChangeSetDst())) {
                    dst2File.put(newFile.getChangeSetDst(), newFile);
                }
            }
            for (ScmResourceFile deletedFile : deletedItems) {
                if (SdkStringUtils.isNotEmpty(deletedFile.getChangeSetDst()) && dst2File.containsKey(deletedFile.getChangeSetDst())) {
                    ScmResourceFile newFile = dst2File.get(deletedFile.getChangeSetDst());
                    deleted2newMovedFiles.put(deletedFile, newFile);
                }
            }

            for (Map.Entry<ScmResourceFile, ScmResourceFile> entry : deleted2newMovedFiles.entrySet()) {
                ScmResourceFile deletedFile = entry.getKey();
                ScmResourceFile newFile = entry.getValue();

                newFile.setIsMoved(true);
                newFile.setOldName(deletedFile.getName());
                newFile.setOldRelativePath(deletedFile.getRelativePath());
                newFile.setOctaneStatus(OctaneStatus.MODIFIED);

                result.getAllScmResourceFiles().remove(deletedFile);
            }
        }
    }

    private AbstractMap.SimpleEntry<AutomatedTest, AutomatedTest> createPairsDeletedNew(List<AutomatedTest> newTests, AutomatedTest deletedTest, UftTestDiscoveryResult result) {

        Map<String, List<String>> combineDataTableHashCodeToTestPathList = result.getCombineDataTableHashCodeToTestPathListMap();
        List<AutomatedTest> deletedTestsList = new LinkedList<>();
        List<AutomatedTest> newTestsList = new LinkedList<>();
        combineDataTableHashCodeToTestPathList.values().forEach(entry -> {
            List<AutomatedTest> tempDeletedTestsList = new LinkedList<>();
            List<AutomatedTest> tempNewTestsList = new LinkedList<>();
            entry.forEach(testPath1 -> {
                List<AutomatedTest> internalDeletedTestsList = new LinkedList<>();
                List<AutomatedTest> internalNewTestsList = new LinkedList<>();

                String deletedTestPath = deletedTest.getPackage() + "\\" + deletedTest.getName();
                if (deletedTestPath.equals(testPath1)) {
                    internalDeletedTestsList.add(deletedTest);
                }
                newTests.forEach(test -> {
                    String newTestPath = test.getPackage() + "\\" + test.getName();
                    if (newTestPath.equals(testPath1)) {
                        internalNewTestsList.add(test);
                    }
                });
                tempDeletedTestsList.addAll(internalDeletedTestsList);
                tempNewTestsList.addAll(internalNewTestsList);

            });
            if (tempDeletedTestsList.size() == 1 && tempNewTestsList.size() == 1) {
                deletedTestsList.addAll(tempDeletedTestsList);
                newTestsList.addAll(tempNewTestsList);
            } else if (tempDeletedTestsList.size() == 1 && tempNewTestsList.size() > 1) {
                String allTestsName = getTestsName(tempNewTestsList);
                logger.warn("The following tests are dupilcated: " + allTestsName + " it is recommended to rename duplicated test one by one and not in bulk.");
            }

        });
        if (deletedTestsList.size() == 1 && newTestsList.size() == 1) {
            return new AbstractMap.SimpleEntry<>(deletedTestsList.get(0), newTestsList.get(0));
        } else {
            return null;
        }
    }

    private String getTestsName(List<AutomatedTest> tempNewTestsList) {
        String returnString = tempNewTestsList.stream().map(test -> test.getName() + ", ").collect(Collectors.joining());
        return returnString.substring(0, returnString.length() - 2);
    }

    /**
     * This method try to find ids of updated and deleted tests for scm change detection
     * if test is found on server - update id of discovered test
     * if test is not found and test is marked for update - move it to new tests (possibly test was deleted on server)
     *
     * @return true if there were changes comparing to discoverede results
     */
    private boolean validateTestDiscoveryAndCompleteTestIdsForScmChangeDetection(EntitiesService entitiesService, UftTestDiscoveryResult result) {
        boolean hasDiff = false;

        Set<String> allTestNames = new HashSet<>();
        for (AutomatedTest test : result.getAllTests()) {
            if (test.getIsMoved()) {
                allTestNames.add(test.getOldName());
            } else {
                allTestNames.add(test.getName());
            }
        }

        //GET TESTS FROM OCTANE
        Collection<String> additionalFields = SdkStringUtils.isNotEmpty(result.getTestRunnerId()) ? Collections.singletonList(EntityConstants.AutomatedTest.TEST_RUNNER_FIELD) : null;
        Map<String, Entity> octaneTestsMapByKey = getTestsFromServer(entitiesService, Long.parseLong(result.getWorkspaceId()), Long.parseLong(result.getScmRepositoryId()), true, allTestNames, additionalFields);

        //MATCHING
        for (AutomatedTest discoveredTest : result.getAllTests()) {
            String key = discoveredTest.getIsMoved()
                    ? createKey(discoveredTest.getOldPackage(), discoveredTest.getOldName())
                    : createKey(discoveredTest.getPackage(), discoveredTest.getName());
            Entity octaneTest = octaneTestsMapByKey.get(key);
            boolean octaneTestFound = (octaneTest != null);
            if (octaneTestFound) {
                discoveredTest.setId(octaneTest.getId());
            }
            switch (discoveredTest.getOctaneStatus()) {
                case DELETED:
                    if (!octaneTestFound) {
                        //discoveredTest that is marked to be deleted - doesn't exist in Octane - do nothing
                        hasDiff = true;
                        discoveredTest.setOctaneStatus(OctaneStatus.NONE);
                    }
                    break;
                case MODIFIED:
                    if (!octaneTestFound) {
                        //updated discoveredTest that has no matching in Octane, possibly was remove from Octane. So we move it to new tests
                        hasDiff = true;
                        discoveredTest.setOctaneStatus(OctaneStatus.NEW);
                    } else {
                        boolean testsEqual = checkTestEquals(discoveredTest, octaneTest, result.getTestRunnerId());
                        if (testsEqual) { //if equal - skip
                            discoveredTest.setOctaneStatus(OctaneStatus.NONE);
                        }
                    }
                    break;
                case NEW:
                    if (octaneTestFound) {
                        //new discoveredTest was found in Octane - move it to update
                        hasDiff = true;
                        discoveredTest.setOctaneStatus(OctaneStatus.MODIFIED);
                    }
                    break;
                default:
                    //do nothing
            }
        }

        return hasDiff;
    }

    private boolean validateTestDiscoveryAndCompleteDataTableIdsForScmChangeDetection(EntitiesService entitiesService, UftTestDiscoveryResult result) {
        boolean hasDiff = false;
        Set<String> allNames = new HashSet<>();
        for (ScmResourceFile file : result.getAllScmResourceFiles()) {
            if (file.getIsMoved()) {
                allNames.add(file.getOldName());
            } else {
                allNames.add(file.getName());
            }
        }

        //GET DataTables FROM OCTANE
        Map<String, Entity> octaneEntityMapByRelativePath = getDataTablesFromServer(entitiesService, Long.parseLong(result.getWorkspaceId()), Long.parseLong(result.getScmRepositoryId()), allNames);

        //MATCHING
        for (ScmResourceFile file : result.getAllScmResourceFiles()) {

            String key = file.getIsMoved() ? file.getOldRelativePath() : file.getRelativePath();
            Entity octaneFile = octaneEntityMapByRelativePath.get(key);

            boolean octaneFileFound = (octaneFile != null);
            if (octaneFileFound) {
                file.setId(octaneFile.getId());
            }

            switch (file.getOctaneStatus()) {
                case DELETED:
                    if (!octaneFileFound) {
                        //file that is marked to be deleted - doesn't exist in Octane - do nothing
                        hasDiff = true;
                        file.setOctaneStatus(OctaneStatus.NONE);
                    }
                    break;
                case MODIFIED:
                    if (!octaneFileFound) {
                        //updated file that has no matching in Octane, possibly was remove from Octane. So we move it to new
                        hasDiff = true;
                        file.setOctaneStatus(OctaneStatus.NEW);
                    }
                    break;
                case NEW:
                    if (octaneFileFound) {
                        //new file was found in Octane - do nothing(there is nothing to update)
                        hasDiff = true;
                        file.setOctaneStatus(OctaneStatus.NONE);
                    }
                    break;
                default:
                    //do nothing
            }
        }

        return hasDiff;
    }

}


