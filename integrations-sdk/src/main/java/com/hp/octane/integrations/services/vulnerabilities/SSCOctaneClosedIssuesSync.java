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
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.dto.securityscans.impl.OctaneIssueImpl;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.rest.SSCRestClient;
import com.hp.octane.integrations.services.vulnerabilities.ssc.Issues;
import com.hp.octane.integrations.services.vulnerabilities.ssc.SscProjectConnector;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.hp.octane.integrations.services.vulnerabilities.SSCHandler.createListNodeEntity;

public class SSCOctaneClosedIssuesSync {

    private final static Logger logger = LogManager.getLogger(SSCOctaneClosedIssuesSync.class);

    private VulnerabilitiesServiceImpl.VulnerabilitiesQueueItem vulnerabilitiesQueueItem;
    private SSCRestClient sscRestClient;
    private SSCProjectConfiguration sscProjectConfiguration;
    private OctaneRestClient octaneRestClient;
    private DTOFactory dtoFactory = DTOFactory.getInstance();
    private OctaneConfiguration octaneConfiguration;

    public SSCOctaneClosedIssuesSync(
            VulnerabilitiesServiceImpl.VulnerabilitiesQueueItem queueItem,
            RestService restService,
            SSCProjectConfiguration sscProjectConfiguration,
            OctaneConfiguration octaneConfig) {

        vulnerabilitiesQueueItem = queueItem;
        this.sscRestClient = restService.obtainSSCRestClient();
        this.sscProjectConfiguration = sscProjectConfiguration;
        this.octaneRestClient = restService.obtainOctaneRestClient();
        this.octaneConfiguration = octaneConfig;
    }

    public List<OctaneIssue> getCloseIssueInSSCOpenedInOctane() {
        List<String> issueRemoteIds = null;
        try {
            issueRemoteIds = getRemoteIdsOpenVulnsFromOctane(vulnerabilitiesQueueItem.jobId,
                    vulnerabilitiesQueueItem.buildId);

            Issues sscIssues = getOpenVulnsFromSSC(sscProjectConfiguration, sscRestClient);
            return getClosedInSSCOpenInOctane(issueRemoteIds, sscIssues);

        } catch (IOException e) {
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
        }
        return new ArrayList<>();
    }


    private List<OctaneIssue> getClosedInSSCOpenInOctane(List<String> octaneIssues, Issues sscIssues) {

        if (sscIssues.getCount() == 0) {
            return new ArrayList<>();
        }
        List<String> remoteIdsSSC =
                sscIssues.getData().stream().map(t -> t.issueInstanceId).collect(Collectors.toList());
        List<String> retVal = new ArrayList<>(octaneIssues);
        retVal.removeAll(remoteIdsSSC);
        //Make Octane issue from remote id's.
        Entity closedListNodeEntity = createListNodeEntity(dtoFactory, "list_node.issue_state_node.closed");
        return retVal.stream().map(t -> {
            OctaneIssueImpl octaneIssue = new OctaneIssueImpl();
            octaneIssue.setRemoteId(t);
            octaneIssue.setState(closedListNodeEntity);
            return octaneIssue;
        }).collect(Collectors.toList());

    }

    private Issues getOpenVulnsFromSSC(SSCProjectConfiguration sscProjectConfiguration, SSCRestClient sscRestClient) {
        SscProjectConnector projectConnector = new SscProjectConnector(sscProjectConfiguration, sscRestClient);
        return projectConnector.readIssues(projectConnector.getProjectVersion().id, "updated");
    }

    private List<String> getRemoteIdsOpenVulnsFromOctane(String jobId, String runId) throws IOException {


        Map<String, String> headers = new HashMap<>();
        headers.put(RestService.CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());

        OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
                .setMethod(HttpMethod.GET)
                .setUrl(getOpenVulnerabilitiesContextPath(octaneConfiguration.getUrl(),
                        octaneConfiguration.getSharedSpace()) +
                        "?instance-id=" + octaneConfiguration.getInstanceId() +
                        String.format("&job-ci-id=%s&build-ci-id=%s&state=open&remote-tag=%s", CIPluginSDKUtils.urlEncodeQueryParam(jobId), CIPluginSDKUtils.urlEncodeQueryParam(runId),CIPluginSDKUtils.urlEncodeQueryParam(sscProjectConfiguration.getRemoteTag())))
                .setHeaders(headers);

        OctaneResponse response = octaneRestClient.execute(request);
        logger.info("vulnerabilities pushed; status: " + response.getStatus() + ", response: " + response.getBody());
        if (response.getStatus() == HttpStatus.SC_OK) {
            logger.info("retrieved existing vulnerabilities from Octane.");
        } else {
            logger.error("Error retrieving existing vulnerabilities from Octane.");
            throw new IOException();
        }
        return CIPluginSDKUtils.getObjectMapper().readValue(response.getBody(), List.class);
    }

    private String getOpenVulnerabilitiesContextPath(String octaneBaseUrl, String sharedSpaceId) {
        return octaneBaseUrl + RestService.SHARED_SPACE_API_PATH_PART + sharedSpaceId + RestService.OPEN_VULNERABILITIES_FROM_OCTANE;
    }
}
