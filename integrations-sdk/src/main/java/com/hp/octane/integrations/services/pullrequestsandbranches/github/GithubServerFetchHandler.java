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

public class GithubServerFetchHandler extends GithubV3FetchHandler {

    public GithubServerFetchHandler(AuthenticationStrategy authenticationStrategy) {
        super(authenticationStrategy);
    }

    @Override
    public String getRepoApiPath(String repoHttpCloneUrl) {
        validateHttpCloneUrl(repoHttpCloneUrl);

        if (repoHttpCloneUrl.toLowerCase().startsWith(CLOUD_SERVICE_PREFIX)) {
            throw new IllegalArgumentException("Supplied repository URL : " + repoHttpCloneUrl + " is Git Cloud URL. Change 'SCM Tool type' to Github Cloud.");
        }
        //   https://github.houston.softwaregrp.net:443/MQM/mqm.git;
        // =>https://github.houston.softwaregrp.net/api/v3/MQM/mqm
        int repoSlashIndex = repoHttpCloneUrl.lastIndexOf("/");
        int teamSlashIndex = repoHttpCloneUrl.substring(0, repoSlashIndex).lastIndexOf("/");
        String result = repoHttpCloneUrl.substring(0, teamSlashIndex) + "/api/v3/repos" + repoHttpCloneUrl.substring(teamSlashIndex);
        result = result.substring(0, result.length() - ".git".length());
        return result;
    }

    @Override
    public String getApiPath(String repoHttpCloneUrl) {
        //   https://github.houston.softwaregrp.net:443/MQM/mqm.git;
        // =>https://github.houston.softwaregrp.net/api/v3
        int repoSlashIndex = repoHttpCloneUrl.lastIndexOf("/");
        int teamSlashIndex = repoHttpCloneUrl.substring(0, repoSlashIndex).lastIndexOf("/");
        String result = repoHttpCloneUrl.substring(0, teamSlashIndex) + "/api/v3";
        return result;
    }
}
