package com.hp.octane.integrations.utils;

import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.entities.EntityConstants;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.entities.QueryHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MbtDiscoveryResultHelper {

    public static boolean isUnitToRunnerRelationDefined(EntitiesService entitiesService, Long workspaceId) {
        String condition1 = QueryHelper.condition(EntityConstants.Base.ENTITY_NAME_FIELD, EntityConstants.ModelFolder.ENTITY_NAME);
        String condition2 = QueryHelper.condition(EntityConstants.Base.NAME_FIELD, EntityConstants.ModelFolder.TEST_RUNNER_FIELD);

        List<Entity> entities = entitiesService.getEntities(workspaceId, "metadata/fields", Arrays.asList(condition1, condition2), Collections.emptyList());
        return !entities.isEmpty();
    }
}
