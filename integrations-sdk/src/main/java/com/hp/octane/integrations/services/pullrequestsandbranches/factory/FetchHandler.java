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

package com.hp.octane.integrations.services.pullrequestsandbranches.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.scm.Branch;
import com.hp.octane.integrations.dto.scm.PullRequest;
import com.hp.octane.integrations.dto.scm.SCMRepositoryLinks;
import com.hp.octane.integrations.services.pullrequestsandbranches.rest.GeneralRestClient;
import com.hp.octane.integrations.services.pullrequestsandbranches.rest.authentication.AuthenticationStrategy;
import org.apache.http.HttpStatus;
import org.apache.http.conn.HttpHostConnectException;
import org.gitlab4j.api.GitLabApi;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class FetchHandler {

    protected static final DTOFactory dtoFactory = DTOFactory.getInstance();
    protected final GeneralRestClient restClient;

    public FetchHandler(AuthenticationStrategy authenticationStrategy) {
        restClient = new GeneralRestClient(authenticationStrategy);
    }

    public abstract List<PullRequest> fetchPullRequests(PullRequestFetchParameters parameters, CommitUserIdPicker commitUserIdPicker, Consumer<String> logger) throws IOException;

    public abstract List<Branch> fetchBranches(BranchFetchParameters parameters, Map<String, Long> sha2DateMapCache, Consumer<String> logger) throws IOException;

    public abstract String getRepoApiPath(String clonePath);

    protected abstract String parseRequestError(OctaneResponse response);

    public SCMRepositoryLinks pingRepository(String repoApiBaseUrl, Consumer<String> logConsumer) throws IOException {
        OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class).setUrl(repoApiBaseUrl).setMethod(HttpMethod.GET);
        try {
            OctaneResponse response = restClient.executeRequest(request);
            if (response.getStatus() == HttpStatus.SC_OK) {
                logConsumer.accept("Ping repository : Ok");
                return parseSCMRepositoryLinks(response.getBody());
            }
            logConsumer.accept("Ping repository : " + response.getStatus() + "; " + parseRequestError(response));
            if (response.getStatus() == HttpStatus.SC_NOT_FOUND) {
                throw new IllegalArgumentException("Repository not found. Please validate that project/user name and repository name are spelled correctly.");
            }
            if (response.getStatus() == HttpStatus.SC_UNAUTHORIZED) {
                throw new IllegalArgumentException("Unauthorized, validate correctness of credentials.");
            }

            throw new IllegalArgumentException("Unexpected exception");

        } catch (HttpHostConnectException e) {
            throw new IOException("Repository is not available. Please validate that URL and proxy settings are set correctly.");
        }
    }

    public abstract SCMRepositoryLinks  parseSCMRepositoryLinks(String responseBody) throws JsonProcessingException;

    protected void validateHttpCloneUrl(String clonePath) {
        if (clonePath == null || clonePath.isEmpty()) {
            throw new IllegalArgumentException("Repo url cannot be empty");
        }

        String clonePathLowerCase = clonePath.toLowerCase();
        String repoSuffix = ".git";

        if (clonePathLowerCase.startsWith("ssh")) {
            throw new IllegalArgumentException("SSH protocol is not supported by this action.");
        }
        if (!clonePath.toLowerCase().startsWith("http")) {
            throw new IllegalArgumentException("Repo url must start with 'http(s)'");
        }
        if (!clonePathLowerCase.endsWith(repoSuffix)) {
            throw new IllegalArgumentException("Repo url must end with '.git'");
        }
    }

    public abstract RepoTemplates buildRepoTemplates(String repoApiBaseUrl);

    public static String getUserName(CommitUserIdPicker idPicker, String email, String name) {
        if (idPicker != null) {
            return idPicker.getUserIdForCommit(email, name);
        }
        return getUserName(email, name);
    }

    public static String getUserName(String email, String name) {
        if (email != null && email.contains("@")) {
            String[] emailParts = email.split("@");
            if (emailParts.length > 1) {
                String id = emailParts[0].trim();
                if (!id.isEmpty()) {
                    return id;
                }
            }
        }

        return name;
    }
}
