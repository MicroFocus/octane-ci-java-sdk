package com.hp.octane.integrations.uft;

import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.entities.EntityConstants;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.entities.QueryHelper;
import com.hp.octane.integrations.uft.items.AutomatedTest;
import com.hp.octane.integrations.uft.items.OctaneStatus;
import com.hp.octane.integrations.uft.items.UftTestAction;
import com.hp.octane.integrations.uft.items.UftTestDiscoveryResult;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static com.hp.octane.integrations.utils.MbtDiscoveryResultHelper.isNewRunner;
import static com.hp.octane.integrations.utils.MbtDiscoveryResultHelper.isUnitToRunnerRelationDefined;

/**
 * @author Itay Karo on 26/08/2021
 */
public class MbtDiscoveryResultPreparerImpl implements DiscoveryResultPreparer {

    private static final Logger logger = LogManager.getLogger(MbtDiscoveryResultPreparerImpl.class);

    private boolean bUnitToRunner;

    @Override
    public void prepareDiscoveryResultForDispatchInFullSyncMode(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
        String conditionRepositoryPathNotEmpty = QueryHelper.conditionNot(QueryHelper.conditionEmpty(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD));
        List<Entity> unitsFromServer;

        bUnitToRunner = isUnitToRunnerRelationDefined(entitiesService, Long.parseLong(discoveryResult.getWorkspaceId())) &&
                isNewRunner(entitiesService, Long.parseLong(discoveryResult.getWorkspaceId()), Long.parseLong(discoveryResult.getTestRunnerId()));
        if (bUnitToRunner) {
            String conditionHasScmRepository = QueryHelper.conditionRef(EntityConstants.MbtUnit.SCM_REPOSITORY_FIELD, Long.parseLong(discoveryResult.getScmRepositoryId()));
            unitsFromServer = getUnitsFromServer(entitiesService, discoveryResult.getWorkspaceId(), conditionRepositoryPathNotEmpty, conditionHasScmRepository);
        } else {
            unitsFromServer = getUnitsFromServer(entitiesService, discoveryResult.getWorkspaceId(), conditionRepositoryPathNotEmpty);
        }

        Map<String, Entity> octaneUnitsMap = unitsFromServer.stream()
                .collect(Collectors.toMap(entity -> entity.getStringValue(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD).toLowerCase(),
                        action -> action,
                        (action1, action2) -> action1)); // remove duplicates TODO (Itay)- remove when repository path uniqueness validation will be added in octane

        removeExistingUnits(discoveryResult, octaneUnitsMap);
    }

    @Override
    public void prepareDiscoveryResultForDispatchInScmChangesMode(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
        // prepare moved tests
        prepareMovedTests(discoveryResult);

        // handle deleted tests
        handleDeletedTests(entitiesService, discoveryResult);

        // handle added tests
        handleAddedTests(entitiesService, discoveryResult);

        // handle updated tests
        handleUpdatedTests(entitiesService, discoveryResult);

        // handle moved tests
        handleMovedTests(entitiesService, discoveryResult);

    }

    private List<Entity> getUnitsFromServer(EntitiesService entitiesService, String workspaceId, String... conditions) {
        List<String> fields = new ArrayList<>(Arrays.asList(EntityConstants.MbtUnit.ID_FIELD, EntityConstants.MbtUnit.NAME_FIELD,
                EntityConstants.MbtUnit.DESCRIPTION_FIELD, EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD));
        if (bUnitToRunner) {
            fields.add(EntityConstants.MbtUnit.TEST_RUNNER_FIELD);
        }

        return entitiesService.getEntities(Long.parseLong(workspaceId), EntityConstants.MbtUnit.COLLECTION_NAME, Arrays.asList(conditions), fields);
    }

    private void removeExistingUnits(UftTestDiscoveryResult discoveryResult, Map<String, Entity> octaneUnitsMap) {
        discoveryResult.getAllTests().forEach(automatedTest -> {
            automatedTest.getActions().forEach(action -> {
                Entity octaneUnit = octaneUnitsMap.get(action.getRepositoryPath().toLowerCase());
                if (Objects.nonNull(octaneUnit)) {
                    if (bUnitToRunner && octaneUnit.getField(EntityConstants.MbtUnit.TEST_RUNNER_FIELD) == null) {
                        action.setOctaneStatus(OctaneStatus.MODIFIED);
                        action.setId(octaneUnit.getId());
                    } else {
                        action.setOctaneStatus(OctaneStatus.NONE);
                    }
                }
            });
            removeItemsWithStatusNone(automatedTest.getActions());
        });

    }

    private void prepareMovedTests(UftTestDiscoveryResult result) {
        List<AutomatedTest> newTests = result.getNewTests();
        List<AutomatedTest> deletedTests = result.getDeletedTests();
        if (!newTests.isEmpty() && !deletedTests.isEmpty()) {
            logger.info("processing moved tests");
            Map<String, AutomatedTest> dst2Test = newTests.stream()
                    .filter(automatedTest -> SdkStringUtils.isNotEmpty(automatedTest.getChangeSetDst()))
                    .collect(Collectors.toMap(AutomatedTest::getChangeSetDst, automatedTest -> automatedTest));

            Map<AutomatedTest, AutomatedTest> deleted2newMovedTests = deletedTests.stream()
                    .filter(automatedTest -> SdkStringUtils.isNotEmpty(automatedTest.getChangeSetDst()) && dst2Test.containsKey(automatedTest.getChangeSetDst()))
                    .collect(Collectors.toMap(automatedTest -> automatedTest, automatedTest -> dst2Test.get(automatedTest.getChangeSetDst())));

            deleted2newMovedTests.forEach((deletedTest, newTest) -> {
                newTest.setIsMoved(true);
                newTest.setOldName(deletedTest.getName());
                newTest.setOldPackage(deletedTest.getPackage());
                newTest.setOctaneStatus(OctaneStatus.MODIFIED);

                result.getAllTests().remove(deletedTest);
            });

            logger.info("found {} tests that were moved", deleted2newMovedTests.size());
        }
    }

    // for deleted tests, we will not delete the relevant units. instead, we will reset some of their attributes
    private void handleDeletedTests(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
        List<AutomatedTest> deletedTests = discoveryResult.getDeletedTests();

        if (CollectionUtils.isEmpty(deletedTests)) {
            return;
        }

        logger.info("processing deleted tests. found {} tests", deletedTests.size());

        // create a condition for each test to fetch its units by the old test name
        String condition = QueryHelper.orConditions(deletedTests.stream()
                .map(test -> getActionPathPrefixCondition(test, false))
                .toArray(String[]::new));

        List<Entity> unitsFromServer = getUnitsFromServer(entitiesService, discoveryResult.getWorkspaceId(), condition);

        // since the test was already deleted from the scm, the automated test will not contain any uft actions. so, we
        // need to map each unit from octane to the automated test and create a marker uft action with only the unit id
        // so later we will be able to update the unit entities
        deletedTests.forEach(automatedTest -> {
            String actionPathPrefix = UftTestDiscoveryUtils.getTestPathPrefix(automatedTest, false);
            // find all the unit entities that belong to this test
            List<Entity> unitEntitiesOfTest = unitsFromServer.stream()
                    .filter(entity -> entity.getStringValue(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD).startsWith(actionPathPrefix))
                    .collect(Collectors.toList());
            unitsFromServer.removeAll(unitEntitiesOfTest);
            // convert unit entities to test actions
            List<UftTestAction> actions = unitEntitiesOfTest.stream().map(entity -> {
                UftTestAction action = convertToAction(entity);
                action.setOctaneStatus(OctaneStatus.DELETED);
                return action;
            }).collect(Collectors.toList());
            automatedTest.setActions(actions);
        });
    }

    private String getActionPathPrefixCondition(AutomatedTest test, boolean orgPath) {
        String actionPathPrefix = UftTestDiscoveryUtils.getTestPathPrefix(test, orgPath);
        return QueryHelper.conditionStartWith(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD, actionPathPrefix);
    }

    private void handleAddedTests(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
        List<AutomatedTest> newTests = discoveryResult.getNewTests();

        if (CollectionUtils.isEmpty(newTests)) {
            return;
        }

        logger.info("processing new tests. found {} tests", newTests.size());

        Set<String> newActionsRepositoryPaths = newTests.stream()
                .filter(automatedTest -> !automatedTest.getIsMoved()) // not moved test
                .map(AutomatedTest::getActions)
                .flatMap(Collection::stream)
                .map(UftTestAction::getRepositoryPath)
                .collect(Collectors.toSet());

        String condition = QueryHelper.conditionIn(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD, newActionsRepositoryPaths, false);
        List<Entity> unitsFromServer = getUnitsFromServer(entitiesService, discoveryResult.getWorkspaceId(), condition);

        Map<String, Entity> octaneUnitsMap = unitsFromServer.stream()
                .collect(Collectors.toMap(entity -> entity.getStringValue(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD).toLowerCase(),
                        action -> action,
                        (action1, action2) -> action1)); // remove duplicates TODO (Itay)- remove when repository path uniqueness validation will be added in octane

        removeExistingUnits(discoveryResult, octaneUnitsMap);
    }

    private void handleUpdatedTests(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
        List<AutomatedTest> updatedTests = discoveryResult.getUpdatedTests().stream()
                .filter(automatedTest -> !automatedTest.getIsMoved())
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(updatedTests)) {
            return;
        }

        logger.info("processing updated tests. found {} tests", updatedTests.size());

        // there are 4 cases:
        // 1. new action- action will exist in the automated test but not in octane
        // 2. delete action- action will exist in octane but not in the automated test
        // 3. updated action- action will exist both in the automated test and in octane and will differ in the logical
        // name and/or description
        // 4. not modified action- action in the automated test is equal to the unit in octane

        Map<String, UftTestAction> scmPathToActionMap = updatedTests.stream().map(AutomatedTest::getActions)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(action -> extractScmPathFromActionPath(action.getRepositoryPath()), action -> action));

        // create a condition for each test to fetch its units by the old test name
        String condition = QueryHelper.orConditions(updatedTests.stream()
                .map(test -> getActionPathPrefixCondition(test, false))
                .toArray(String[]::new));

        List<Entity> unitsFromServer = getUnitsFromServer(entitiesService, discoveryResult.getWorkspaceId(), condition);
        Map<String, Entity> scmPathToEntityMap = unitsFromServer.stream()
                .collect(Collectors.toMap(entity -> extractScmPathFromActionPath(entity.getStringValue(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD)),
                        action -> action,
                        (action1, action2) -> action1)); // remove duplicates TODO (Itay)- remove when repository path uniqueness validation will be added in octane

        handleUpdatedTestAddedActionCase(scmPathToActionMap, scmPathToEntityMap);

        handleUpdatedTestDeletedActionCase(scmPathToActionMap, scmPathToEntityMap, updatedTests);

        handleUpdatedTestUpdatedActionCase(scmPathToActionMap, scmPathToEntityMap);

        // just a validation
        if (!scmPathToActionMap.isEmpty() || !scmPathToEntityMap.isEmpty()) {
            logger.warn("not all of the existing units or actions were processed");
        }

    }

    private void handleMovedTests(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
        List<AutomatedTest> movedTests = discoveryResult.getUpdatedTests().stream()
                .filter(AutomatedTest::getIsMoved)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(movedTests)) {
            return;
        }

        // create a condition for each test to fetch its units by the old test name
        String condition = QueryHelper.orConditions(movedTests.stream()
                .map(test -> getActionPathPrefixCondition(test, true))
                .toArray(String[]::new));

        List<Entity> unitsFromServer = getUnitsFromServer(entitiesService, discoveryResult.getWorkspaceId(), condition);

        // now, we need to match between the original units and the automated test by the original action path prefix.
        // then, we will store a mapping between what the new action path should be and the unit id and update each unit
        // with the id and update the status to modified
        Map<String, String> actionPathToUnitIdMap = new HashMap<>();
        movedTests.forEach(automatedTest -> {
            // match units from octane to automated test by the original repository path prefix
            String originalActionPathPrefix = UftTestDiscoveryUtils.getTestPathPrefix(automatedTest, true);
            String newActionPathPrefix = UftTestDiscoveryUtils.getTestPathPrefix(automatedTest, false);
            List<Entity> entities = unitsFromServer.stream()
                    .filter(entity -> entity.getStringValue(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD).startsWith(originalActionPathPrefix))
                    .collect(Collectors.toList());

            // for each entity, replace the original repository path prefix with the new one
            entities.forEach(entity -> {
                String key = entity.getStringValue(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD).replace(originalActionPathPrefix, newActionPathPrefix);
                actionPathToUnitIdMap.put(key, entity.getId());
            });

            // remove the matched entities to remove duplications
            unitsFromServer.removeAll(entities);
        });

        // when we reach here all the units from octane should have been removed
        if (CollectionUtils.isNotEmpty(unitsFromServer)) {
            logger.warn("not all units from octane were mapped to moved tests");
        }

        // scan all moved tests and update the actions with the ids from octane
        movedTests.forEach(automatedTest -> automatedTest.getActions().forEach(action -> {
            action.setId(actionPathToUnitIdMap.get(action.getRepositoryPath()));
            action.setOctaneStatus(OctaneStatus.MODIFIED);
            action.setMoved(true);
            action.setOldTestName(automatedTest.getOldName());
            action.getParameters().forEach(parameter -> parameter.setOctaneStatus(OctaneStatus.NONE));
            removeItemsWithStatusNone(action.getParameters());
        }));
    }

    // the action path is in the form of <test package>\<test name>\<action name>:<action logical name | action name>.
    // this method extracts the scm path (from the beginning until the ':' character)
    private String extractScmPathFromActionPath(String repositoryPath) {
        int index = repositoryPath.indexOf(":");
        if (index == -1) {
            return repositoryPath;
        } else {
            return repositoryPath.substring(0, index).toLowerCase();
        }
    }

    // handle case 1 for added actions
    private void handleUpdatedTestAddedActionCase(Map<String, UftTestAction> scmPathToActionMap, Map<String, Entity> scmPathToEntityMap) {
        Collection<String> addedActions = CollectionUtils.removeAll(scmPathToActionMap.keySet(), scmPathToEntityMap.keySet());
        if (CollectionUtils.isNotEmpty(addedActions)) {
            logger.info("found {} updated tests for added action", addedActions.size());

            addedActions.forEach(s -> {
                scmPathToActionMap.get(s).setOctaneStatus(OctaneStatus.NEW); // not required, just for readability
                scmPathToActionMap.remove(s);
            });
        }
    }

    // handle case 2 for deleted actions
    private void handleUpdatedTestDeletedActionCase(Map<String, UftTestAction> scmPathToActionMap, Map<String, Entity> scmPathToEntityMap, List<AutomatedTest> updatedTests) {
        Collection<String> deletedActions = CollectionUtils.removeAll(scmPathToEntityMap.keySet(), scmPathToActionMap.keySet());
        if (CollectionUtils.isNotEmpty(deletedActions)) {
            Set<AutomatedTest> updatedTestsCounter = new HashSet<>();
            deletedActions.forEach(s -> {
                String scmTestPath = extractScmTestPath(s);
                if (Objects.isNull(scmTestPath)) {
                    logger.warn("repository path {} of unit id {} name {} is not valid and will be discarded", s, scmPathToEntityMap.get(s).getId(), scmPathToEntityMap.get(s).getName());
                    scmPathToEntityMap.remove(s);
                } else {
                    // try to match between the automated test and the units to be deleted. since the action was already deleted
                    // from the scm, we need to update octane. the handling is the same as handling a deleted test. we need
                    // to mark the deleted actions and provide only the unit id
                    updatedTests.forEach(automatedTest -> {
                        String calculatedTestPath = UftTestDiscoveryUtils.getTestPathPrefix(automatedTest, false).toLowerCase();
                        // match found. add a marker action to the automated test
                        if (calculatedTestPath.equals(scmTestPath)) {
                            Entity entity = scmPathToEntityMap.get(s);
                            UftTestAction action = convertToAction(entity);
                            action.setOctaneStatus(OctaneStatus.DELETED);
                            automatedTest.getActions().add(action);
                            updatedTestsCounter.add(automatedTest);
                        }
                    });
                    scmPathToEntityMap.remove(s);

                    logger.info("found {} updated tests for deleted action", updatedTestsCounter.size());
                }
            });
        }
    }

    // handle case 3 for updated actions
    private void handleUpdatedTestUpdatedActionCase(Map<String, UftTestAction> scmPathToActionMap, Map<String, Entity> scmPathToEntityMap) {
        // updated action candidates
        Collection<String> sameActions = CollectionUtils.retainAll(scmPathToEntityMap.keySet(), scmPathToActionMap.keySet());
        if (CollectionUtils.isNotEmpty(sameActions)) {
            sameActions.forEach(scmPath -> {
                UftTestAction action = scmPathToActionMap.get(scmPath);
                // current functionality does not listen to name and description changes. in order to support this, uncomment
                // the section below
/*
                Entity entity = scmPathToEntityMap.get(scmPath);
                if(!isEquals(action, entity)) {
                    action.setId(entity.getId());
                    action.setOctaneStatus(OctaneStatus.MODIFIED);
                } else {
*/
                action.setOctaneStatus(OctaneStatus.NONE);
                action.getParameters().forEach(parameter -> parameter.setOctaneStatus(OctaneStatus.NONE)); // currently do not support parameter changes
                // }
                scmPathToActionMap.remove(scmPath);
                scmPathToEntityMap.remove(scmPath);
            });
        }
    }

    private boolean isEquals(UftTestAction action, Entity unitEntity) {
        String unitName = unitEntity.getStringValue(EntityConstants.MbtUnit.NAME_FIELD);
        String unitDescription = unitEntity.getStringValue(EntityConstants.MbtUnit.DESCRIPTION_FIELD);
        if (SdkStringUtils.isNotEmpty(unitDescription)) { // clear html tags from octane
            unitDescription = removeHtmlTagsFromString(unitDescription);
        }

        return !unitName.toLowerCase().contains(action.getName().toLowerCase()) && // action name was not action number like Action4
                action.getLogicalName().equalsIgnoreCase(unitName) &&
                ((SdkStringUtils.isEmpty(action.getDescription()) && SdkStringUtils.isEmpty(unitDescription)) ||
                        (SdkStringUtils.isNotEmpty(action.getDescription()) && SdkStringUtils.isNotEmpty(unitDescription) && action.getDescription().equalsIgnoreCase(unitDescription)));
    }

    // a valid path is of the form <test package>\<test name>\<action name>:<action logical name | action name>. an invalid
    // form can be caused if a user manually entered an scm path in-correctly
    // this method extracts the test path from the repostory path. if not valid returns null
    private String extractScmTestPath(String scmPath) {
        scmPath = extractScmPathFromActionPath(scmPath);
        int index = scmPath.lastIndexOf('\\');
        if (index == -1) {
            return null;
        } else {
            String scmTestPath = scmPath.substring(0, index);
            String actionNumber = scmPath.substring(index + 1, scmPath.length() - 1);
            // the last part of the test path should contain the action name like "action10"
            if (actionNumber.startsWith("action")) {
                return scmTestPath;
            } else {
                return null;
            }
        }
    }

    private String removeHtmlTagsFromString(String str) {
        if (SdkStringUtils.isEmpty(str)) {
            return str;
        }

        str = str.replace("<p>", "\n");
        return str.replaceAll("\\<.*?>", "");
    }

    private UftTestAction convertToAction(Entity entity) {
        UftTestAction action = new UftTestAction();
        action.setId(entity.getId());
        action.setName(entity.getName());
        action.setLogicalName(entity.getName());
        action.setRepositoryPath(String.valueOf(entity.getField(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD)));
        return action;
    }

}
