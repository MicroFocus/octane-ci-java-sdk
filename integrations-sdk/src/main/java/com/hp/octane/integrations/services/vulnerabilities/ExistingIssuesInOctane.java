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

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 * com.hp.mqm.analytics.devops.insights.resources.DevopsInsightsSSAPublicApiResource#getRemoteIdsOfIssueOfSiblingRuns
 */
public class ExistingIssuesInOctane {

    private final static Logger logger = LogManager.getLogger(ExistingIssuesInOctane.class);
    private OctaneRestClient octaneRestClient;
    private OctaneConfiguration octaneConfiguration;

    public ExistingIssuesInOctane(OctaneRestClient octaneRestClient,
                                  OctaneConfiguration octaneConfiguration) {

        this.octaneRestClient = octaneRestClient;
        this.octaneConfiguration = octaneConfiguration;
    }

    public List<String> getRemoteIdsOpenVulnsFromOctane(String jobId, String runId, String remoteTag) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(RestService.CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());

        boolean base64 = ConfigurationParameterFactory.isEncodeCiJobBase64(octaneConfiguration);
        String encodedJobId = base64 ? CIPluginSDKUtils.urlEncodeBase64(jobId) : CIPluginSDKUtils.urlEncodeQueryParam(jobId);

        String url = getOpenVulnerabilitiesContextPath(octaneConfiguration.getUrl(),
                octaneConfiguration.getSharedSpace()) +
                "?instance-id=" + octaneConfiguration.getInstanceId() +
                String.format("&job-ci-id=%s&build-ci-id=%s&state=open&remote-tag=%s",
                        encodedJobId,
                        CIPluginSDKUtils.urlEncodeQueryParam(runId),
                        CIPluginSDKUtils.urlEncodeQueryParam(remoteTag));
        if (base64) {
            url = CIPluginSDKUtils.addParameterEncode64ToUrl(url);
        }

        OctaneRequest request = DTOFactory.getInstance().newDTO(OctaneRequest.class)
                .setMethod(HttpMethod.GET)
                .setUrl(url)
                .setHeaders(headers);


        OctaneResponse response = octaneRestClient.execute(request);
        logger.info(octaneConfiguration.getLocationForLog() + "vulnerabilities retrieve was completed; status: " + response.getStatus() + ", response: " + response.getBody());
        if (response.getStatus() == HttpStatus.SC_OK) {
            logger.info(octaneConfiguration.getLocationForLog() + "retrieved existing vulnerabilities from Octane.");
        } else {
            logger.error(octaneConfiguration.getLocationForLog() + "Error retrieving existing vulnerabilities from Octane.");
            throw new IOException();
        }
        return CIPluginSDKUtils.getObjectMapper().readValue(response.getBody(), List.class);
    }

    private String getOpenVulnerabilitiesContextPath(String octaneBaseUrl, String sharedSpaceId) {
        return octaneBaseUrl + RestService.SHARED_SPACE_API_PATH_PART +
                sharedSpaceId + RestService.OPEN_VULNERABILITIES_FROM_OCTANE;
    }
}
