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

package com.hp.octane.integrations.dto.executor.impl;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.DTOInternalProviderBase;
import com.hp.octane.integrations.dto.executor.*;

/**
 */

public final class DTOExecutorsProvider extends DTOInternalProviderBase {

    public DTOExecutorsProvider(DTOFactory.DTOConfiguration configuration) {
        dtoPairs.put(DiscoveryInfo.class, DiscoveryInfoImpl.class);
        dtoPairs.put(TestSuiteExecutionInfo.class, TestSuiteExecutionInfoImpl.class);
        dtoPairs.put(TestExecutionInfo.class, TestExecutionInfoImpl.class);
        dtoPairs.put(TestConnectivityInfo.class, TestConnectivityInfoImpl.class);
        dtoPairs.put(CredentialsInfo.class, CredentialsInfoImpl.class);
    }

    protected <T extends DTOBase> T instantiateDTO(Class<T> targetType) throws InstantiationException, IllegalAccessException {
        T result = null;
        if (dtoPairs.containsKey(targetType)) {
            result = (T) dtoPairs.get(targetType).newInstance();
        }
        return result;
    }
}
