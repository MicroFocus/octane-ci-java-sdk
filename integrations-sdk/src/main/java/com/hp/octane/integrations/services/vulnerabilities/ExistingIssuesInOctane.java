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
package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
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

public class ExistingIssuesInOctane {

    private final static Logger logger = LogManager.getLogger(ExistingIssuesInOctane.class);
    private OctaneRestClient octaneRestClient;
    private OctaneConfiguration octaneConfiguration;
    private VulnerabilitiesServiceImpl.VulnerabilitiesQueueItem queueItem;

    public ExistingIssuesInOctane(OctaneRestClient octaneRestClient,
                                  OctaneConfiguration octaneConfiguration,
                                  VulnerabilitiesServiceImpl.VulnerabilitiesQueueItem queueItem) {

        this.octaneRestClient = octaneRestClient;
        this.octaneConfiguration = octaneConfiguration;
        this.queueItem = queueItem;
    }
    public List<String> getRemoteIdsOpenVulnsFromOctane(String jobId, String runId, String remoteTag) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(RestService.CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());

        OctaneRequest request = DTOFactory.getInstance().newDTO(OctaneRequest.class)
                .setMethod(HttpMethod.GET)
                .setUrl(getOpenVulnerabilitiesContextPath(octaneConfiguration.getUrl(),
                        octaneConfiguration.getSharedSpace()) +
                        "?instance-id=" + octaneConfiguration.getInstanceId() +
                        String.format("&job-ci-id=%s&build-ci-id=%s&state=open&remote-tag=%s", CIPluginSDKUtils.urlEncodeQueryParam(jobId), CIPluginSDKUtils.urlEncodeQueryParam(runId),
                                CIPluginSDKUtils.urlEncodeQueryParam(remoteTag)))
                .setHeaders(headers);


        OctaneResponse response = octaneRestClient.execute(request);
        logger.info("vulnerabilities retrieve was completed; status: " + response.getStatus() + ", response: " + response.getBody());
        if (response.getStatus() == HttpStatus.SC_OK) {
            logger.info("retrieved existing vulnerabilities from Octane.");
        } else {
            logger.error("Error retrieving existing vulnerabilities from Octane.");
            throw new IOException();
        }
        return CIPluginSDKUtils.getObjectMapper().readValue(response.getBody(), List.class);
    }

    private String getOpenVulnerabilitiesContextPath(String octaneBaseUrl, String sharedSpaceId) {
        return octaneBaseUrl + RestService.SHARED_SPACE_API_PATH_PART +
                sharedSpaceId + RestService.OPEN_VULNERABILITIES_FROM_OCTANE;
    }
}
