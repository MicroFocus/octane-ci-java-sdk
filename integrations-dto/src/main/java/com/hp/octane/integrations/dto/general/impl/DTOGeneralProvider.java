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

package com.hp.octane.integrations.dto.general.impl;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.DTOInternalProviderBase;
import com.hp.octane.integrations.dto.general.*;

/**
 * General purposes DTOs definitions, mostly configuration related ones
 */

public final class DTOGeneralProvider extends DTOInternalProviderBase {

	public DTOGeneralProvider(DTOFactory.DTOConfiguration configuration) {
		super(configuration);
		dtoPairs.put(CIPluginInfo.class, CIPluginInfoImpl.class);
		dtoPairs.put(CIServerInfo.class, CIServerInfoImpl.class);
		dtoPairs.put(CIPluginSDKInfo.class, CIPluginSDKInfoImpl.class);
		dtoPairs.put(CIProviderSummaryInfo.class, CIProviderSummaryInfoImpl.class);
		dtoPairs.put(CIJobsList.class, CIJobsListImpl.class);
		dtoPairs.put(Taxonomy.class, TaxonomyImpl.class);
		dtoPairs.put(ListItem.class, ListItemImpl.class);
	}

	protected <T extends DTOBase> T instantiateDTO(Class<T> targetType) throws InstantiationException, IllegalAccessException {
		T result = null;
		if (dtoPairs.containsKey(targetType)) {
			result = (T) dtoPairs.get(targetType).newInstance();
		}
		return result;
	}
}
