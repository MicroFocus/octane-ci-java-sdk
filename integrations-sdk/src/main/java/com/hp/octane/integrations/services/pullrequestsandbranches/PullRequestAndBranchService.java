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
package com.hp.octane.integrations.services.pullrequestsandbranches;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.scm.PullRequest;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.*;
import com.hp.octane.integrations.services.rest.RestService;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;


public interface PullRequestAndBranchService {

    String  PULL_REQUEST_COLLECTION_SUPPORTED_VERSION = "15.0.40";
    String  BRANCH_COLLECTION_SUPPORTED_VERSION = "15.1.80";
    String  BRANCH_TEMPLATE_SUPPORTED_VERSION = "15.1.40";

    /**
     * Service instance producer - for internal usage only (protected by inaccessible configurer)
     *
     * @param configurer  SDK services configurer object
     * @param restService Rest Service
     * @param entitiesService Entities Service
     * @return initialized service
     */
    static PullRequestAndBranchService newInstance(OctaneSDK.SDKServicesConfigurer configurer, RestService restService, EntitiesService entitiesService) {
        return new PullRequestAndBranchServiceImpl(configurer, restService, entitiesService);
    }

    void sendPullRequests(List<PullRequest> pullRequests, String workspaceId, PullRequestFetchParameters pullRequestFetchParameters, Consumer<String> logConsumer) throws IOException;

    long getPullRequestLastUpdateTime(String workspaceId, String repoUrl);

    BranchSyncResult syncBranchesToOctane(FetchHandler fetcherHandler, BranchFetchParameters fp, Long workspaceId, CommitUserIdPicker idPicker, Consumer<String> logConsumer) throws IOException;

    /**
     * Update repo templates. This method doesn't override existing values in ALM Octane
     * @param repoUrl repo url to search in ALM Octane. Case sensitive.
     * @param workspaceId workspace in ALM Octane
     * @param templates templates to set. Not must include all templates.
     * @return true if repo url found and templates are updated
     */
    boolean updateRepoTemplates(String repoUrl, Long workspaceId, RepoTemplates templates);
}
