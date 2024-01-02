/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
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
        dtoPairs.put(CIBranchesList.class, CIBranchesListImpl.class);
        dtoPairs.put(Taxonomy.class, TaxonomyImpl.class);
        dtoPairs.put(ListItem.class, ListItemImpl.class);
        dtoPairs.put(MbtUnit.class, MbtUnitImpl.class);
        dtoPairs.put(MbtData.class, MbtDataImpl.class);
        dtoPairs.put(MbtUnitParameter.class, MbtUnitParameterImpl.class);
        dtoPairs.put(MbtDataTable.class, MbtDataTableImpl.class);

        dtoPairs.put(CIBuildStatusInfo.class, CIBuildStatusInfoImpl.class);
    }

    protected <T extends DTOBase> T instantiateDTO(Class<T> targetType) throws InstantiationException, IllegalAccessException {
        T result = null;
        if (dtoPairs.containsKey(targetType)) {
            result = (T) dtoPairs.get(targetType).newInstance();
        }
        return result;
    }
}
