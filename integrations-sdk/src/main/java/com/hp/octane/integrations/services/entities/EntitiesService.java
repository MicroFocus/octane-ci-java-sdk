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
package com.hp.octane.integrations.services.entities;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.entities.ResponseEntityList;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.dto.entities.Entity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface EntitiesService {

	/**
	 * Service instance producer - for internal usage only (protected by inaccessible configurer)
	 *
	 * @param configurer  SDK services configurer object
	 * @param restService Rest Service
	 * @return initialized service
	 */
	static EntitiesService newInstance(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
		return new EntitiesServiceImpl(configurer, restService);
	}

	List<Entity> postEntities(Long workspaceId, String entityCollectionName, List<Entity> entities);

	List<Entity> postEntities(Long workspaceId, String entityCollectionName, List<Entity> entities, Collection<String> fields);

	List<Entity> postEntities(Long workspaceId, String entityCollectionName, List<Entity> entities, Collection<String> fields, Map<String,String> serviceArgument);

	List<Entity> postEntities(Long workspaceId, String entityCollectionName, String jsonData);

	List<Entity> postEntities(Long workspaceId, String entityCollectionName, String jsonData, Collection<String> fields);

	List<Entity> postEntities(Long workspaceId, String entityCollectionName, String jsonData, Collection<String> fields,Map<String,String> serviceArgument);

	ResponseEntityList getPagedEntities(String url);

	List<Entity> getEntities(Long workspaceId, String entityCollectionName, Collection<String> conditions, Collection<String> fields);

	List<Entity> getEntities(Long workspaceId, String entityCollectionName, Collection<String> conditions, String orderBy, Collection<String> fields);

	List<Entity> getEntitiesByIds(Long workspaceId, String collectionName, Collection<?> ids);

	List<Entity> getEntitiesByIds(Long workspaceId, String collectionName, Collection<?> ids, Collection<String> fields);

	List<Entity> deleteEntitiesByIds(Long workspaceId, String entityCollectionName, Collection<?> entitiesIds);

	List<Entity> deleteEntities(Long workspaceId, String entityCollectionName, Collection<String> conditions);

	List<Entity> updateEntities(Long workspaceId, String entityCollectionName, List<Entity> entities);

	List<Entity> updateEntities(Long workspaceId, String entityCollectionName, String jsonData);

	String buildEntityUrl(Long workspaceId, String collection, Collection<String> conditions, Collection<String> fields, Integer offset, Integer limit, String orderBy);

	String buildEntityUrl(Long workspaceId, String collection, Collection<String> conditions, Collection<String> fields, Integer offset, Integer limit, String orderBy,Map<String,String> serviceArgument);

}
