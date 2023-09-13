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

package com.hp.octane.integrations.uft;

import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.uft.items.CustomLogger;
import com.hp.octane.integrations.uft.items.JobRunContext;
import com.hp.octane.integrations.uft.items.UftTestDiscoveryResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class UftTestDispatchUtils {

    private static final Logger logger = LogManager.getLogger(UftTestDispatchUtils.class);

    private static Map<TestingToolType, DiscoveryResultPreparer> preparersMap = new HashMap<>();

    private static Map<TestingToolType, DiscoveryResultDispatcher> dispatchersMap = new HashMap<>();

    static {
        preparersMap.put(TestingToolType.UFT, new UftDiscoveryResultPreparerImpl());
        preparersMap.put(TestingToolType.MBT, new MbtDiscoveryResultPreparerImpl());
        dispatchersMap.put(TestingToolType.UFT, new UftDiscoveryResultDispatcherImpl());
        dispatchersMap.put(TestingToolType.MBT, new MbtDiscoveryResultDispatcherImpl());
    }

    public static void prepareDiscoveryResultForDispatch(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult) {
        logger.info("Prepare discovery results before dispatching for: {}, full sync: {} ", discoveryResult.getTestingToolType(), discoveryResult.isFullScan());
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
