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
package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.ClosableService;
import com.hp.octane.integrations.services.HasMetrics;
import com.hp.octane.integrations.services.HasQueueService;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.queueing.QueueingService;

import java.util.Map;

public interface VulnerabilitiesService extends ClosableService, HasQueueService, HasMetrics {

    /**
     * Register custom vulnerabilities tool service
     * @param vulnerabilitiesToolService vulnerabilities tool service instance
     */
    void addVulnerabilitiesToolService(VulnerabilitiesToolService vulnerabilitiesToolService);

    /**
     * Service instance producer - for internal usage only (protected by inaccessible configurer)
     *
     * @param queueingService             queueingService
     * @param vulnerabilitiesToolServices vulnerabilitiesToolServices
     * @param configurer                  configurer
     * @param restService                 restService
     * @param configurationService        Configuration Service
     * @return return initialized service
     */
    static VulnerabilitiesService newInstance(QueueingService queueingService, VulnerabilitiesToolService[] vulnerabilitiesToolServices,
                                              OctaneSDK.SDKServicesConfigurer configurer, RestService restService, ConfigurationService configurationService) {
        return new VulnerabilitiesServiceImpl(queueingService, vulnerabilitiesToolServices, configurer, restService, configurationService);
    }

    /**
     * Enqueue retrieve and push vulnerabilities scan
     * This is the preferred way to push vulnerabilities scan results to Octane. This method provides facilities of queue, non-main thread execution and retry.
     *
     * @param jobId                any identification of Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
     * @param buildId              any identification of Build or the specified above Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
     * @param toolType             toolType
     * @param startRunTime         timestamp of build start
     * @param queueItemTimeout     timeout defined for this queue item
     * @param additionalProperties additionalProperties
     * @param rootJobId            rootJobId
     */
    void enqueueRetrieveAndPushVulnerabilities(String jobId,
                                               String buildId,
                                               ToolType toolType,
                                               long startRunTime,
                                               long queueItemTimeout,
                                               Map<String, String> additionalProperties,
                                               String rootJobId);

    /**
     * Enqueue retrieve and push vulnerabilities scan
     * This is the preferred way to push vulnerabilities scan results to Octane. This method provides facilities of queue, non-main thread execution and retry.
     *
     * @param jobId                any identification of Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
     * @param buildId              any identification of Build or the specified above Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
     * @param toolType             toolType
     * @param startRunTime         timestamp of build start
     * @param queueItemTimeout     timeout defined for this queue item
     * @param additionalProperties additionalProperties
     * @param rootJobId            rootJobId
     */
    void enqueueRetrieveAndPushVulnerabilities(String jobId,
                                               String buildId,
                                               String toolType,
                                               long startRunTime,
                                               long queueItemTimeout,
                                               Map<String, String> additionalProperties,
                                               String rootJobId);
}
