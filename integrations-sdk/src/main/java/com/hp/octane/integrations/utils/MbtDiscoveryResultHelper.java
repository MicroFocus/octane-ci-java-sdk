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
    private static boolean unitToRunnerRelationDefined(EntitiesService entitiesService, Long workspaceId) {
        String condition1 = QueryHelper.condition(EntityConstants.Base.ENTITY_NAME_FIELD, EntityConstants.ModelFolder.ENTITY_NAME);
        String condition2 = QueryHelper.condition(EntityConstants.Base.NAME_FIELD, EntityConstants.ModelFolder.TEST_RUNNER_FIELD);

        List<Entity> entities = entitiesService.getEntities(workspaceId, "metadata/fields", Arrays.asList(condition1, condition2), Collections.emptyList());
        return !entities.isEmpty();
    }

    public static boolean newRunnerEnabled(EntitiesService entitiesService, Long workspaceId, String runnerId) {
        if (SdkStringUtils.isNotEmpty(runnerId) && unitToRunnerRelationDefined(entitiesService, workspaceId)) {
            return getRunnerDedicatedFolder(entitiesService, workspaceId, runnerId) != null;
        } else {
            return false;
        }
    }

    public static Entity getRunnerDedicatedFolder(EntitiesService entitiesService, long workspaceId, String runnerId) {
        String condition1 = QueryHelper.conditionRef(EntityConstants.ModelFolder.TEST_RUNNER_FIELD, Long.parseLong(runnerId));
        String condition2 = QueryHelper.condition(EntityConstants.ModelFolder.SUBTYPE_FIELD, EntityConstants.ModelFolder.ENTITY_SUBTYPE);

        List<Entity> entities = entitiesService.getEntities(workspaceId, EntityConstants.ModelFolder.COLLECTION_NAME, Arrays.asList(condition1, condition2), Collections.emptyList());
        return entities.stream().findFirst().orElse(null);
    }


}
