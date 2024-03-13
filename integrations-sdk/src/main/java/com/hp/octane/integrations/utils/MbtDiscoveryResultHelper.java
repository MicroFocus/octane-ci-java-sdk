package com.hp.octane.integrations.utils;

import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.entities.EntityConstants;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.entities.QueryHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MbtDiscoveryResultHelper {


    /**
     * This method is MBT workaround defined to support DEV and Prod environments simultaneously
     * On Jenkins side we can't know if Octane experiment is ON or OFF, so we cant know if the required field is present on Octane entity
     * This function gets from Octane entity metadata the required field data.
     * If the field exists the Octane returns the field properties
     * Else we are getting an empty properties list
     * This way we know to not send this field within our updates or to not ask for its value
     *
     * @param entitiesService - service connects to Octane
     * @param workspaceId - the current WS in Octane
     * @return - if required field properties are exists
     */
    public static boolean isUnitToRunnerRelationDefined(EntitiesService entitiesService, Long workspaceId) {
        String condition1 = QueryHelper.condition(EntityConstants.Base.ENTITY_NAME_FIELD, EntityConstants.ModelFolder.ENTITY_NAME);
        String condition2 = QueryHelper.condition(EntityConstants.Base.NAME_FIELD, EntityConstants.ModelFolder.TEST_RUNNER_FIELD);

        List<Entity> entities = entitiesService.getEntities(workspaceId, "metadata/fields", Arrays.asList(condition1, condition2), Collections.emptyList());
        return !entities.isEmpty();
    }
}
