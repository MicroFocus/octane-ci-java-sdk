package com.hp.octane.integrations.uft;

import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.entities.EntityConstants;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.entities.QueryHelper;
import com.hp.octane.integrations.uft.items.*;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Itay Karo on 26/08/2021
 */
public class MbtDiscoveryResultDispatcherImpl extends DiscoveryResultDispatcher {

    private static final Logger logger = LogManager.getLogger(MbtDiscoveryResultDispatcherImpl.class);

    private static final Entity TESTING_TOOL_TYPE = createListNodeEntity("list_node.bu_testing_tool_type.uft");

    private static final Entity AUTOMATED_AUTOMATION_STATUS = createListNodeEntity("list_node.automation_status.automated");

    private static final Entity NOT_AUTOMATED_AUTOMATION_STATUS = createListNodeEntity("list_node.automation_status.not_automated");

    private static final Entity INPUT_PARAMETER_TYPE = createListNodeEntity("list_node.entity_parameter_type.input");

    private static final Entity OUTPUT_PARAMETER_TYPE = createListNodeEntity("list_node.entity_parameter_type.output");

    @Override
    public void dispatchDiscoveryResults(EntitiesService entitiesService, UftTestDiscoveryResult result, JobRunContext jobRunContext, CustomLogger customLogger) {
        List<UftTestAction> allActions = result.getAllTests().stream()
                .map(AutomatedTest::getActions)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        long workspaceId = Long.parseLong(result.getWorkspaceId());
        String runnerId = result.getTestRunnerId();
        String scmRepositoryId = result.getScmRepositoryId();

        Map<OctaneStatus, List<UftTestAction>> actionsByStatusMap = allActions.stream().collect(Collectors.groupingBy(UftTestAction::getOctaneStatus));

        Entity autoDiscoveredFolder = null;
        if (CollectionUtils.isNotEmpty(actionsByStatusMap.get(OctaneStatus.NEW)) || CollectionUtils.isNotEmpty(actionsByStatusMap.get(OctaneStatus.MODIFIED))) {
            autoDiscoveredFolder = retrieveParentFolder(entitiesService, workspaceId, Long.parseLong(runnerId));
        }

        // handle new actions- create new units and parameters in octane
        dispatchNewActions(entitiesService, actionsByStatusMap.get(OctaneStatus.NEW), autoDiscoveredFolder, workspaceId, runnerId, scmRepositoryId, customLogger);

        // handle deleted actions- currently do nothing
        dispatchDeletedActions(entitiesService, actionsByStatusMap.get(OctaneStatus.DELETED), workspaceId, customLogger);

        // handle updated actions- update units in octane
        dispatchUpdatedActions(entitiesService, actionsByStatusMap.get(OctaneStatus.MODIFIED), autoDiscoveredFolder, workspaceId, runnerId, scmRepositoryId, customLogger);
    }

    private void dispatchNewActions(EntitiesService entitiesService, List<UftTestAction> newActions, Entity autoDiscoveredFolder, long workspaceId, String runnerId, String scmRepositoryId, CustomLogger customLogger) {
        if (CollectionUtils.isNotEmpty(newActions)) {
            Map<String, Entity> foldersMap = createParentFolders(entitiesService, newActions, autoDiscoveredFolder, workspaceId);
            postUnits(entitiesService, newActions, foldersMap, workspaceId, runnerId, scmRepositoryId);
        }
    }

    private Map<String, Entity> createParentFolders(EntitiesService entitiesService, List<UftTestAction> newActions, Entity autoDiscoveredFolder, long workspaceId) {
        // find existing sub folders. each folder's name is the test name that contains the actions
        List<Entity> existingSubFolders = retrieveChildFolders(entitiesService, workspaceId, Long.parseLong(autoDiscoveredFolder.getId()));
        Map<String, Entity> existingSubFoldersMap = existingSubFolders.stream().collect(Collectors.toMap(Entity::getName, entity -> entity));

        Set<String> testNames = newActions.stream().map(UftTestAction::getTestName).collect(Collectors.toSet());
        // find which folders are missing and need to be created
        testNames.removeAll(existingSubFoldersMap.keySet());
        if (CollectionUtils.isNotEmpty(testNames)) {
            Map<String, Entity> newFoldersMap = postFolders(entitiesService, testNames, autoDiscoveredFolder, workspaceId);
            existingSubFoldersMap.putAll(newFoldersMap);
        }

        return existingSubFoldersMap;
    }

    private void dispatchDeletedActions(EntitiesService entitiesService, List<UftTestAction> deletedActions, long workspaceId, CustomLogger customLogger) {
        if (CollectionUtils.isNotEmpty(deletedActions)) {
            deleteUnits(entitiesService, deletedActions, workspaceId);
        }
    }

    private void dispatchUpdatedActions(EntitiesService entitiesService, List<UftTestAction> updatedActions, Entity autoDiscoveredFolder, long workspaceId, String runnerId, String scmRepositoryId, CustomLogger customLogger) {
        if (CollectionUtils.isNotEmpty(updatedActions)) {
            updateParentFolders(entitiesService, updatedActions, autoDiscoveredFolder, workspaceId);
            updateUnits(entitiesService, updatedActions, workspaceId, runnerId, scmRepositoryId);
        }
    }

    // update the name of unit's folders after a test was moved
    private void updateParentFolders(EntitiesService entitiesService, List<UftTestAction> updatedActions, Entity autoDiscoveredFolder, long workspaceId) {
        Map<String, String> oldNameToNewName = updatedActions.stream().filter(action -> action.isMoved() && !action.getTestName().equals(action.getOldTestName()))
                .collect(Collectors.toMap(UftTestAction::getOldTestName, UftTestAction::getTestName, (s1, s2) -> s1));

        if (!oldNameToNewName.isEmpty()) {
            String condition1 = QueryHelper.conditionRef(EntityConstants.ModelFolder.PARENT, Long.parseLong(autoDiscoveredFolder.getId()));
            String condition2 = QueryHelper.conditionIn(EntityConstants.ModelFolder.NAME_FIELD, oldNameToNewName.keySet(), false);
            List<Entity> foldersToUpdate = entitiesService.getEntities(workspaceId, EntityConstants.ModelFolder.COLLECTION_NAME, Arrays.asList(condition1, condition2), Arrays.asList(EntityConstants.ModelFolder.ID_FIELD, EntityConstants.ModelFolder.NAME_FIELD));
            updateFolders(entitiesService, foldersToUpdate, oldNameToNewName, workspaceId);
        }
    }

    private Map<String, Entity> postFolders(EntitiesService entitiesService, Set<String> names, Entity parentFolder, long workspaceId) {
        if (!names.isEmpty()) {
            logger.info("dispatching {} new folders", names.size());

            // add folders
            List<Entity> foldersToAdd = names.stream().map(name -> createFolderEntity(name, parentFolder)).collect(Collectors.toList());
            Map<String, Entity> folderEntities = entitiesService.postEntities(workspaceId, EntityConstants.ModelFolder.COLLECTION_NAME, foldersToAdd, Collections.singletonList(EntityConstants.Base.NAME_FIELD)).stream()
                    .collect(Collectors.toMap(Entity::getName, Function.identity()));

            logger.info("actual new folders {} added", folderEntities.size());

            return folderEntities;
        }

        return Collections.emptyMap();
    }

    private void updateFolders(EntitiesService entitiesService, List<Entity> foldersToUpdate, Map<String, String> oldNameToNewName, long workspaceId) {
        if (!foldersToUpdate.isEmpty()) {
            logger.info("dispatching {} updated folders", foldersToUpdate.size());

            // update folders
            foldersToUpdate = foldersToUpdate.stream().map(entity -> dtoFactory.newDTO(Entity.class).setId(entity.getId()).setName(oldNameToNewName.get(entity.getName())))
                    .collect(Collectors.toList());
            List<Entity> folderEntities = entitiesService.updateEntities(workspaceId, EntityConstants.ModelFolder.COLLECTION_NAME, foldersToUpdate);

            logger.info("actual updated folders {}", folderEntities.size());
        }
    }

    private boolean postUnits(EntitiesService entitiesService, List<UftTestAction> actions, Map<String, Entity> foldersMap, long workspaceId, String runnerId, String scmRepositoryId) {
        if (!actions.isEmpty()) {
            logger.info("dispatching {} new units", actions.size());

            List<Entity> parameterToAdd = new ArrayList<>(); // add external parameter entities list to be filled by each action creation
            // add units
            List<Entity> unitsToAdd = actions.stream().map(action -> createUnitEntity(action, foldersMap, parameterToAdd, runnerId, scmRepositoryId)).collect(Collectors.toList());
            Map<String, Entity> unitEntities = entitiesService.postEntities(workspaceId, EntityConstants.MbtUnit.COLLECTION_NAME, unitsToAdd, Collections.singletonList(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD)).stream()
                    .collect(Collectors.toMap(entity -> entity.getField(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD).toString(), Function.identity()));

            logger.info("actual new units {} added", unitEntities.size());

            // replace parent unit entities for parameters in order to save their relations
            logger.info("dispatching {} new unit parameters", parameterToAdd.size());
            parameterToAdd.forEach(parameter -> {
                Entity parentUnit = (Entity) parameter.getField(EntityConstants.MbtUnitParameter.MODEL_ITEM);
                Entity newParentUnit = unitEntities.get(parentUnit.getField(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD).toString());
                parameter.setField(EntityConstants.MbtUnitParameter.MODEL_ITEM, createModelItemEntity(newParentUnit));
            });
            // add parameters
            List<Entity> unitParameterEntities = entitiesService.postEntities(workspaceId, EntityConstants.MbtUnitParameter.COLLECTION_NAME, parameterToAdd);
            logger.info("actual new unit parameters {} added", unitParameterEntities.size());

        }
        return true;
    }

    // we do not delete units. instead, we reset some of their attributes
    private boolean deleteUnits(EntitiesService entitiesService, List<UftTestAction> actions, long workspaceId) {
        if (!actions.isEmpty()) {
            logger.info("dispatching {} deleted units", actions.size());
            // convert actions to dtos
            List<Entity> unitsToUpdate = actions.stream().map(action -> dtoFactory.newDTO(Entity.class)
                            .setId(action.getId())
                            .setField(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD, null)
                            .setField(EntityConstants.MbtUnit.AUTOMATION_STATUS_FIELD, NOT_AUTOMATED_AUTOMATION_STATUS)
                            .setField(EntityConstants.MbtUnit.TESTING_TOOL_TYPE_FIELD, null)
                            .setField(EntityConstants.MbtUnit.SCM_REPOSITORY_FIELD, null)
                            .setField(EntityConstants.MbtUnit.TEST_RUNNER_FIELD, null)
                    )
                    .collect(Collectors.toList());

            // update units
            List<Entity> deletedUnitEntities = entitiesService.updateEntities(workspaceId, EntityConstants.MbtUnit.COLLECTION_NAME, unitsToUpdate);
            logger.info("actual deleted units {} updated", deletedUnitEntities.size());
        }
        return true;
    }

    private boolean updateUnits(EntitiesService entitiesService, List<UftTestAction> actions, long workspaceId, String runnerId, String scmRepositoryId) {
        if (!actions.isEmpty()) {
            logger.info("dispatching {} updated units", actions.size());

            Entity scmRepository = dtoFactory.newDTO(Entity.class).setType(EntityConstants.ScmRepository.ENTITY_NAME).setId(scmRepositoryId);
            Entity testRunner = dtoFactory.newDTO(Entity.class).setType(EntityConstants.Executors.ENTITY_NAME).setId(runnerId);

            // convert actions to dtos
            List<Entity> unitsToUpdate = actions.stream().map(action -> {
                        String unitName = SdkStringUtils.isEmpty(action.getLogicalName()) || action.getLogicalName().startsWith("Action") ? action.getTestName() + ":" + action.getName() : action.getLogicalName();
                        return dtoFactory.newDTO(Entity.class)
                                .setId(action.getId())
                                .setField(EntityConstants.MbtUnit.NAME_FIELD, unitName)
                                .setField(EntityConstants.MbtUnit.DESCRIPTION_FIELD, action.getDescription())
                                .setField(EntityConstants.MbtUnit.SCM_REPOSITORY_FIELD, scmRepository)
                                .setField(EntityConstants.MbtUnit.TEST_RUNNER_FIELD, testRunner)
                                .setField(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD, action.getRepositoryPath());
                    })
                    .collect(Collectors.toList());

            // update units
            List<Entity> updatedUnitEntities = entitiesService.updateEntities(workspaceId, EntityConstants.MbtUnit.COLLECTION_NAME, unitsToUpdate);
            logger.info("actual updated units {}", updatedUnitEntities.size());
        }
        return true;
    }

    public Entity retrieveParentFolder(EntitiesService entitiesService, long workspaceId, long executorId) {
        Entity autoDiscoveredFolder = getRunnerDedicatedFolder(entitiesService, workspaceId, executorId);

        if (autoDiscoveredFolder == null) {
            // backward compatibility for previously created cloud runners, that have no dedicated folder
            autoDiscoveredFolder = getGitMirrorFolder(entitiesService, workspaceId);
        }

        return autoDiscoveredFolder;
    }

    private Entity getRunnerDedicatedFolder(EntitiesService entitiesService, long workspaceId, long executorId) {
        String condition1 = QueryHelper.conditionRef(EntityConstants.ModelFolder.TEST_RUNNER_FIELD, executorId);
        String condition2 = QueryHelper.condition(EntityConstants.ModelFolder.SUBTYPE_FIELD, EntityConstants.ModelFolder.ENTITY_SUBTYPE);

        List<Entity> entities = entitiesService.getEntities(workspaceId, EntityConstants.ModelFolder.COLLECTION_NAME, Arrays.asList(condition1, condition2), Collections.emptyList());
        return entities.stream().findFirst().orElse(null);
    }

    private Entity getGitMirrorFolder(EntitiesService entitiesService, long workspaceId) {
        String condition = QueryHelper.condition(EntityConstants.ModelFolder.LOGICAL_NAME, "mbt.discovery.unit.default_folder_name");

        List<Entity> entities = entitiesService.getEntities(workspaceId, EntityConstants.ModelFolder.COLLECTION_NAME, Collections.singletonList(condition), Collections.emptyList());
        return entities.stream().findFirst().orElse(null);
    }

    private List<Entity> retrieveChildFolders(EntitiesService entitiesService, long workspaceId, long parentFolderId) {
        String condition1 = QueryHelper.conditionRef(EntityConstants.ModelFolder.PARENT, parentFolderId);
        String condition2 = QueryHelper.condition(EntityConstants.ModelFolder.SUBTYPE_FIELD, EntityConstants.ModelFolder.ENTITY_SUBTYPE);

        return entitiesService.getEntities(workspaceId, EntityConstants.ModelFolder.COLLECTION_NAME, Arrays.asList(condition1, condition2), Collections.emptyList());
    }

    private Entity createFolderEntity(String name, Entity parentFolder) {
        return dtoFactory.newDTO(Entity.class)
                .setType(EntityConstants.ModelFolder.ENTITY_NAME)
                .setField(EntityConstants.ModelFolder.SUBTYPE_FIELD, EntityConstants.ModelFolder.ENTITY_SUBTYPE)
                .setField(EntityConstants.ModelFolder.NAME_FIELD, name)
                .setField(EntityConstants.ModelFolder.PARENT, parentFolder);
    }

    private Entity createUnitEntity(UftTestAction action, Map<String, Entity> foldersMap, List<Entity> parameterEntities, String runnerId, String scmRepositoryId) {
        String unitName = SdkStringUtils.isEmpty(action.getLogicalName()) || action.getLogicalName().startsWith("Action") ? action.getTestName() + ":" + action.getName() : action.getLogicalName();
        Entity parentFolder = foldersMap.get(action.getTestName());
        Entity scmRepository = dtoFactory.newDTO(Entity.class).setType(EntityConstants.ScmRepository.ENTITY_NAME).setId(scmRepositoryId);
        Entity testRunner = dtoFactory.newDTO(Entity.class).setType(EntityConstants.Executors.ENTITY_NAME).setId(runnerId);

        Entity unitEntity = dtoFactory.newDTO(Entity.class)
                .setType(EntityConstants.MbtUnit.ENTITY_NAME)
                .setField(EntityConstants.MbtUnit.SUBTYPE_FIELD, EntityConstants.MbtUnit.ENTITY_SUBTYPE)
                .setField(EntityConstants.MbtUnit.NAME_FIELD, unitName)
                .setField(EntityConstants.MbtUnit.DESCRIPTION_FIELD, action.getDescription())
                .setField(EntityConstants.MbtUnit.PARENT, parentFolder)
                .setField(EntityConstants.MbtUnit.AUTOMATION_STATUS_FIELD, AUTOMATED_AUTOMATION_STATUS)
                .setField(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD, action.getRepositoryPath())
                .setField(EntityConstants.MbtUnit.SCM_REPOSITORY_FIELD, scmRepository)
                .setField(EntityConstants.MbtUnit.TEST_RUNNER_FIELD, testRunner)
                .setField(EntityConstants.MbtUnit.TESTING_TOOL_TYPE_FIELD, TESTING_TOOL_TYPE);

        if (CollectionUtils.isNotEmpty(action.getParameters())) {
            List<Entity> parameters = action.getParameters().stream().map(parameter -> createUnitParameterEntity(parameter, unitEntity)).collect(Collectors.toList());
            parameterEntities.addAll(parameters);
        }

        return unitEntity;
    }

    private Entity createUnitParameterEntity(UftTestParameter parameter, Entity parentUnit) {
        Entity parameterType = null;
        if (UftParameterDirection.INPUT.equals(parameter.getDirection())) {
            parameterType = INPUT_PARAMETER_TYPE;
        } else if (UftParameterDirection.OUTPUT.equals(parameter.getDirection())) {
            parameterType = OUTPUT_PARAMETER_TYPE;
        }

        return dtoFactory.newDTO(Entity.class)
                .setType(EntityConstants.MbtUnitParameter.ENTITY_NAME)
                .setField(EntityConstants.MbtUnitParameter.SUBTYPE_FIELD, EntityConstants.MbtUnitParameter.ENTITY_SUBTYPE)
                .setField(EntityConstants.MbtUnitParameter.NAME_FIELD, parameter.getName())
                .setField(EntityConstants.MbtUnitParameter.MODEL_ITEM, parentUnit)
                .setField(EntityConstants.MbtUnitParameter.TYPE, parameterType)
                .setField(EntityConstants.MbtUnitParameter.DEFAULT_VALUE, parameter.getDefaultValue());
    }

}
