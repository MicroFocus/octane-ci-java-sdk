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
package com.hp.octane.integrations.dto.pipelines.impl;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.DTOInternalProviderBase;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.dto.securityscans.impl.OctaneIssueImpl;

public class DTOIssueProvider  extends DTOInternalProviderBase {

    public DTOIssueProvider(DTOFactory.DTOConfiguration configuration) {
        super(configuration);
        dtoPairs.put(OctaneIssue.class, OctaneIssueImpl.class);
    }

    @Override
    protected <T extends DTOBase> T instantiateDTO(Class<T> targetType) throws InstantiationException, IllegalAccessException {
        T result = null;
        if (dtoPairs.containsKey(targetType)) {
            result = (T) dtoPairs.get(targetType).newInstance();
        }
        return result;
    }
}
