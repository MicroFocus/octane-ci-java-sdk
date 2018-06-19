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

package com.hp.octane.integrations.api;

import com.hp.octane.integrations.dto.entities.Entity;

import java.util.Collection;
import java.util.List;

public interface EntitiesService {

    List<Entity> postEntities(Long workspaceId, String entityCollectionName, List<Entity> entities);

    List<Entity> postEntities(Long workspaceId, String entityCollectionName, String jsonData);

    List<Entity> getEntities(Long workspaceId, String entityCollectionName, Collection<String> conditions, Collection<String> fields);

    List<Entity> deleteEntitiesByIds(Long workspaceId, String entityCollectionName, Collection<?> entitiesIds);

    List<Entity> deleteEntities(Long workspaceId, String entityCollectionName, Collection<String> conditions);

    List<Entity> updateEntities(Long workspaceId, String entityCollectionName, List<Entity> entities);

    List<Entity> updateEntities(Long workspaceId, String entityCollectionName, String jsonData);

}
