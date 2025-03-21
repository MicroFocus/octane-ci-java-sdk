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
package com.hp.octane.integrations.services.pullrequestsandbranches.github;


import com.hp.octane.integrations.services.pullrequestsandbranches.rest.authentication.AuthenticationStrategy;

import java.util.Arrays;
import java.util.List;

public class GithubCloudFetchHandler extends GithubV3FetchHandler {

    public GithubCloudFetchHandler(AuthenticationStrategy authenticationStrategy) {
        super(authenticationStrategy);
    }

    @Override
    public String getRepoApiPath(String repoHttpCloneUrl) {
        validateHttpCloneUrl(repoHttpCloneUrl);


        //"  https://github.com/jenkinsci/hpe-application-automation-tools-plugin.git";
        // =>https://api.github.com/repos/jenkinsci/hpe-application-automation-tools-plugin
        if (!repoHttpCloneUrl.toLowerCase().startsWith(CLOUD_SERVICE_PREFIX)) {
            throw new IllegalArgumentException("Unexpected github cloud repository URL : " + repoHttpCloneUrl + ". Git Cloud URL must start with : https://github.com/. ");
        }
        List<String> parts = Arrays.asList(repoHttpCloneUrl.trim().substring(CLOUD_SERVICE_PREFIX.length()).split("/"));
        if (parts.size() != 2) {
            throw new IllegalArgumentException("Unexpected github cloud repository URL : " + repoHttpCloneUrl + ". Expected format : https://github.com/<user_name>/<repo_name>.git");
        }

        String user = parts.get(parts.size() - 2);
        String repoName = parts.get(parts.size() - 1);
        repoName = repoName.substring(0, repoName.length() - ".git".length());
        return String.format("https://api.github.com/repos/%s/%s", user, repoName);
    }

    @Override
    public String getApiPath(String repoHttpCloneUrl) {
        return "https://api.github.com";
    }
}
