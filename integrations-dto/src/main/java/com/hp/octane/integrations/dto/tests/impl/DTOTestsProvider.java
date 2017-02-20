/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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

package com.hp.octane.integrations.dto.tests.impl;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.DTOInternalProviderBase;
import com.hp.octane.integrations.dto.tests.BuildContext;
import com.hp.octane.integrations.dto.tests.TestRun;
import com.hp.octane.integrations.dto.tests.TestRunError;
import com.hp.octane.integrations.dto.tests.TestsResult;

/**
 * Octane oriented test result DTOs definitions provider
 */

public final class DTOTestsProvider extends DTOInternalProviderBase {

	public DTOTestsProvider(DTOFactory.DTOConfiguration configuration) {
		dtoPairs.put(BuildContext.class, BuildContextImpl.class);
		dtoPairs.put(TestRunError.class, TestRunErrorImpl.class);
		dtoPairs.put(TestRun.class, TestRunImpl.class);
		dtoPairs.put(TestsResult.class, TestsResultImpl.class);

		xmlAbles.add(BuildContextImpl.class);
		xmlAbles.add(TestRunErrorImpl.class);
		xmlAbles.add(TestRunImpl.class);
		xmlAbles.add(TestsResultImpl.class);
	}

	protected <T extends DTOBase> T instantiateDTO(Class<T> targetType) throws InstantiationException, IllegalAccessException {
		T result = null;
		if (dtoPairs.containsKey(targetType)) {
			result = (T) dtoPairs.get(targetType).newInstance();
		}
		return result;
	}
}
