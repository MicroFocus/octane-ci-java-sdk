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

package com.hp.octane.integrations.dto.connectivity.impl;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.DTOInternalProviderBase;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.connectivity.OctaneResultAbridged;
import com.hp.octane.integrations.dto.connectivity.OctaneTaskAbridged;
import com.hp.octane.integrations.dto.connectivity.TaskProcessingErrorBody;
import com.hp.octane.integrations.dto.general.OctaneConnectivityStatus;
import com.hp.octane.integrations.dto.general.impl.OctaneConnectivityStatusImpl;

/**
 * Connectivity related DTOs definitions provider
 */

public final class DTOConnectivityProvider extends DTOInternalProviderBase {

	public DTOConnectivityProvider(DTOFactory.DTOConfiguration configuration) {
		super(configuration);
		dtoPairs.put(OctaneRequest.class, OctaneRequestImpl.class);
		dtoPairs.put(OctaneResponse.class, OctaneResponseImpl.class);
		dtoPairs.put(OctaneTaskAbridged.class, OctaneTaskAbridgedImpl.class);
		dtoPairs.put(OctaneResultAbridged.class, OctaneResultAbridgedImpl.class);
		dtoPairs.put(TaskProcessingErrorBody.class, TaskProcessingErrorBodyImpl.class);
		dtoPairs.put(OctaneConnectivityStatus.class, OctaneConnectivityStatusImpl.class);
	}

	protected <T extends DTOBase> T instantiateDTO(Class<T> targetType) throws InstantiationException, IllegalAccessException {
		T result = null;
		if (dtoPairs.containsKey(targetType)) {
			result = (T) dtoPairs.get(targetType).newInstance();
		}
		return result;
	}
}
