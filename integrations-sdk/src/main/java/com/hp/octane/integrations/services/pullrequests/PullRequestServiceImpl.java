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

package com.hp.octane.integrations.services.pullrequests;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.scm.PullRequest;
import com.hp.octane.integrations.services.rest.RestService;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of tests service
 */

final class PullRequestServiceImpl implements PullRequestService {
    private static final Logger logger = LogManager.getLogger(PullRequestServiceImpl.class);
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();

    private final OctaneSDK.SDKServicesConfigurer configurer;
    private final RestService restService;


    PullRequestServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
        if (configurer == null) {
            throw new IllegalArgumentException("invalid configurer");
        }
        if (restService == null) {
            throw new IllegalArgumentException("rest service MUST NOT be null");
        }
        this.configurer = configurer;
        this.restService = restService;
        logger.info(configurer.octaneConfiguration.geLocationForLog() + "initialized SUCCESSFULLY");
    }


    @Override
    public void sendPullRequests(List<PullRequest> pullRequests, String workspaceId) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(RestService.CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());
        String json = dtoFactory.dtoCollectionToJson(pullRequests);
        OctaneRequest octaneRequest = dtoFactory.newDTO(OctaneRequest.class)
                .setMethod(HttpMethod.PUT)
                .setUrl(configurer.octaneConfiguration.getUrl() +
                        RestService.SHARED_SPACE_API_PATH_PART + configurer.octaneConfiguration.getSharedSpace() +
                        "/workspaces/" + workspaceId + RestService.ANALYTICS_CI_PATH_PART + "pull-requests/")
                .setHeaders(headers)
                .setBody(json);

        OctaneResponse octaneResponse = restService.obtainOctaneRestClient().execute(octaneRequest);
        if (octaneResponse.getStatus() != 200) {
            throw new RuntimeException("Failed to sendPullRequests : (" + octaneResponse.getStatus() + ")" + octaneResponse.getBody());
        }
    }
}
