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

        Map<OctaneStatus, List<UftTestAction>> actionsByStatusMap = allActions.stream().collect(Collectors.groupingBy(UftTestAction::getOctaneStatus));

        // handle new actions- create new units and parameters in octane
        dispatchNewActions(entitiesService, actionsByStatusMap.get(OctaneStatus.NEW), workspaceId, customLogger);

        // handle deleted actions- currently do nothing
        dispatchDeletedActions(entitiesService, actionsByStatusMap.get(OctaneStatus.DELETED), workspaceId, customLogger);

        // handle updated actions- update units in octane
        dispatchUpdatedActions(entitiesService, actionsByStatusMap.get(OctaneStatus.MODIFIED), workspaceId, customLogger);
    }

    private void dispatchNewActions(EntitiesService entitiesService, List<UftTestAction> newActions, long workspaceId, CustomLogger customLogger) {
        if (CollectionUtils.isNotEmpty(newActions)) {
            postUnits(entitiesService, newActions, workspaceId);
        }
    }

    private void dispatchDeletedActions(EntitiesService entitiesService, List<UftTestAction> deletedActions, long workspaceId, CustomLogger customLogger) {
        if (CollectionUtils.isNotEmpty(deletedActions)) {
            deleteUnits(entitiesService, deletedActions, workspaceId);
        }
    }

    private void dispatchUpdatedActions(EntitiesService entitiesService, List<UftTestAction> updatedActions, long workspaceId, CustomLogger customLogger) {
        if (CollectionUtils.isNotEmpty(updatedActions)) {
            updateUnits(entitiesService, updatedActions, workspaceId);
        }
    }

    private boolean postUnits(EntitiesService entitiesService, List<UftTestAction> actions, long workspaceId) {
        if (!actions.isEmpty()) {
            logger.debug("dispatching {} new units", actions.size());

            Entity parentFolder = retrieveParentFolder(entitiesService, workspaceId);

            List<Entity> parameterToAdd = new ArrayList<>(); // add external parameter entities list to be filled by each action creation
            // add units
            List<Entity> unitsToAdd = actions.stream().map(action -> createUnitEntity(action, parentFolder, parameterToAdd)).collect(Collectors.toList());
            Map<String, Entity> unitEntities = entitiesService.postEntities(workspaceId, EntityConstants.MbtUnit.COLLECTION_NAME, unitsToAdd, Arrays.asList(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD)).stream()
                    .collect(Collectors.toMap(entity -> entity.getField(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD).toString(), Function.identity()));

            logger.debug("actual new units {} added", unitEntities.size());

            // replace parent unit entities for parameters in order to save their relations
            logger.debug("dispatching {} new unit parameters", parameterToAdd.size());
            parameterToAdd.forEach(parameter -> {
                Entity parentUnit = (Entity) parameter.getField(EntityConstants.MbtUnitParameter.MODEL_ITEM);
                Entity newParentUnit = unitEntities.get(parentUnit.getField(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD).toString());
                parameter.setField(EntityConstants.MbtUnitParameter.MODEL_ITEM, createModelItemEntity(newParentUnit));
            });
            // add parameters
            List<Entity> unitParameterEntities = entitiesService.postEntities(workspaceId, EntityConstants.MbtUnitParameter.COLLECTION_NAME, parameterToAdd);
            logger.debug("actual new unit parameters {} added", unitParameterEntities.size());

        }
        return true;
    }

    // we do not delete units. instead, we reset some of their attributes
    private boolean deleteUnits(EntitiesService entitiesService, List<UftTestAction> actions, long workspaceId) {
        if (!actions.isEmpty()) {
            logger.debug("dispatching {} deleted units", actions.size());
            // convert actions to dtos
            List<Entity> unitsToUpdate = actions.stream().map(action -> dtoFactory.newDTO(Entity.class)
                    .setId(action.getId())
                    .setField(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD, null)
                    .setField(EntityConstants.MbtUnit.AUTOMATION_STATUS_FIELD, NOT_AUTOMATED_AUTOMATION_STATUS)
                    .setField(EntityConstants.MbtUnit.TESTING_TOOL_TYPE_FIELD, null)
            )
                    .collect(Collectors.toList());

            // update units
            List<Entity> deletedUnitEntities = entitiesService.updateEntities(workspaceId, EntityConstants.MbtUnit.COLLECTION_NAME, unitsToUpdate);
            logger.debug("actual deleted units {} updated", deletedUnitEntities.size());
        }
        return true;
    }

    private boolean updateUnits(EntitiesService entitiesService, List<UftTestAction> actions, long workspaceId) {
        if (!actions.isEmpty()) {
            logger.debug("dispatching {} updated units", actions.size());
            // convert actions to dtos
            List<Entity> unitsToUpdate = actions.stream().map(action -> {
                String unitName = SdkStringUtils.isEmpty(action.getLogicalName()) || action.getLogicalName().startsWith("Action") ? action.getTestName() + ":" + action.getName() : action.getLogicalName();
                return dtoFactory.newDTO(Entity.class)
                        .setId(action.getId())
                        .setField(EntityConstants.MbtUnit.NAME_FIELD, unitName)
                        .setField(EntityConstants.MbtUnit.DESCRIPTION_FIELD, action.getDescription())
                        .setField(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD, action.getRepositoryPath());
            })
                    .collect(Collectors.toList());

            // update units
            List<Entity> updatedUnitEntities = entitiesService.updateEntities(workspaceId, EntityConstants.MbtUnit.COLLECTION_NAME, unitsToUpdate);
            logger.debug("actual updated units {}", updatedUnitEntities.size());
        }
        return true;
    }

    private Entity retrieveParentFolder(EntitiesService entitiesService, long workspaceId) {
        String condition = QueryHelper.condition(EntityConstants.ModelFolder.LOGICAL_NAME, "mbt.discovery.unit.default_folder_name");

        List<Entity> entities = entitiesService.getEntities(workspaceId, EntityConstants.ModelFolder.COLLECTION_NAME, Collections.singletonList(condition), Collections.emptyList());
        return entities.get(0);
    }

    private Entity createUnitEntity(UftTestAction action, Entity parentFolder, List<Entity> parameterEntities) {
        String unitName = SdkStringUtils.isEmpty(action.getLogicalName()) || action.getLogicalName().startsWith("Action") ? action.getTestName() + ":" + action.getName() : action.getLogicalName();

        Entity unitEntity = dtoFactory.newDTO(Entity.class)
                .setType(EntityConstants.MbtUnit.ENTITY_NAME)
                .setField(EntityConstants.MbtUnit.SUBTYPE_FIELD, EntityConstants.MbtUnit.ENTITY_SUBTYPE)
                .setField(EntityConstants.MbtUnit.NAME_FIELD, unitName)
                .setField(EntityConstants.MbtUnit.DESCRIPTION_FIELD, action.getDescription())
                .setField(EntityConstants.MbtUnit.PARENT, parentFolder)
                .setField(EntityConstants.MbtUnit.AUTOMATION_STATUS_FIELD, AUTOMATED_AUTOMATION_STATUS)
                .setField(EntityConstants.MbtUnit.REPOSITORY_PATH_FIELD, action.getRepositoryPath())
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
