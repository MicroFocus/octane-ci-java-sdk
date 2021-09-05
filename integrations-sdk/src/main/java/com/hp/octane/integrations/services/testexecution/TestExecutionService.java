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
 */

package com.hp.octane.integrations.services.testexecution;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.rest.RestService;

import java.io.IOException;
import java.util.List;


public interface TestExecutionService {


    /**
     * Service instance producer - for internal usage only (protected by inaccessible configurer)
     *
     * @param configurer      SDK services configurer object
     * @param restService     Rest Service
     * @param entitiesService Entities Service
     * @return initialized service
     */
    static TestExecutionService newInstance(OctaneSDK.SDKServicesConfigurer configurer, RestService restService, EntitiesService entitiesService) {
        return new TestExecutionServiceImpl(configurer, restService, entitiesService);
    }

    void executeSuiteRuns(Long myWorkspaceIdAsLong, List<Long> suiteIds, Long optionalReleaseId, String optionalSuiteRunName) throws IOException;

}
