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

package com.hp.octane.integrations.uft;

import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.uft.items.CustomLogger;
import com.hp.octane.integrations.uft.items.JobRunContext;
import com.hp.octane.integrations.uft.items.UftTestDiscoveryResult;

import java.util.HashMap;
import java.util.Map;

public class UftTestDispatchUtils {

    private static Map<TestingToolType, DiscoveryResultPreparer> preparersMap = new HashMap<>();

    private static Map<TestingToolType, DiscoveryResultDispatcher> dispatchersMap = new HashMap<>();

    static {
        preparersMap.put(TestingToolType.UFT, new UftDiscoveryResultPreparerImpl());
        preparersMap.put(TestingToolType.MBT, new MbtDiscoveryResultPreparerImpl());
        dispatchersMap.put(TestingToolType.UFT, new UftDiscoveryResultDispatcherImpl());
        dispatchersMap.put(TestingToolType.MBT, new MbtDiscoveryResultDispatcherImpl());
    }

    public static void prepareDiscoveryResultForDispatch(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
        if (discoveryResult.isFullScan()) {
            getDiscoveryResultPreparer(discoveryResult.getTestingToolType()).prepareDiscoveryResultForDispatchInFullSyncMode(entitiesService, discoveryResult);
        } else {
            getDiscoveryResultPreparer(discoveryResult.getTestingToolType()).prepareDiscoveryResultForDispatchInScmChangesMode(entitiesService, discoveryResult);
        }
    }

    public static void dispatchDiscoveryResult(EntitiesService entitiesService, UftTestDiscoveryResult result, JobRunContext jobRunContext, CustomLogger customLogger) {
        getDiscoveryResultDispatcher(result.getTestingToolType()).dispatchDiscoveryResults(entitiesService, result, jobRunContext, customLogger);
    }

    private static DiscoveryResultPreparer getDiscoveryResultPreparer(TestingToolType testingToolType) {
        return preparersMap.get(testingToolType);
    }

    private static DiscoveryResultDispatcher getDiscoveryResultDispatcher(TestingToolType testingToolType) {
        return dispatchersMap.get(testingToolType);
    }

}
