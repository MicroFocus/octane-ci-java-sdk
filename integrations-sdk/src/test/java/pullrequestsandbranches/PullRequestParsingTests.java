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
package pullrequestsandbranches;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PullRequestParsingTests {

    @Test
    public void testBitbucketServerBranchesParsing() throws IOException {
        String json = readResourceAsString("bitbucketServerBranches.json");
        com.hp.octane.integrations.services.pullrequestsandbranches.bitbucketserver.pojo.EntityCollection<com.hp.octane.integrations.services.pullrequestsandbranches.bitbucketserver.pojo.Branch> list =
                com.hp.octane.integrations.services.pullrequestsandbranches.bitbucketserver.JsonConverter.convertCollection(
                json,
                com.hp.octane.integrations.services.pullrequestsandbranches.bitbucketserver.pojo.Branch.class);
        Assert.assertEquals(10, list.getSize());

    }

    @Test
    public void testBitbucketServerPullRequestsParsing() throws IOException {
        String json = readResourceAsString("bitbucketServerPullRequests.json");
        com.hp.octane.integrations.services.pullrequestsandbranches.bitbucketserver.pojo.EntityCollection<com.hp.octane.integrations.services.pullrequestsandbranches.bitbucketserver.pojo.PullRequest> list = com.hp.octane.integrations.services.pullrequestsandbranches.bitbucketserver.JsonConverter.convertCollection(
                json,
                com.hp.octane.integrations.services.pullrequestsandbranches.bitbucketserver.pojo.PullRequest.class);
        Assert.assertEquals(11, list.getSize());

    }

    @Test
    public void testGithubServerPullRequestsParsing() throws IOException {
        String json = readResourceAsString("githubServerPullRequests.json");
        List<com.hp.octane.integrations.services.pullrequestsandbranches.github.pojo.PullRequest> list = com.hp.octane.integrations.services.pullrequestsandbranches.github.JsonConverter.convertCollection(json, com.hp.octane.integrations.services.pullrequestsandbranches.github.pojo.PullRequest.class);
        Assert.assertEquals(5, list.size());
    }

    @Test
    public void testGithubCloudPullRequestsParsing() throws IOException {
        String json = readResourceAsString("githubCloudPullRequests.json");
        List<com.hp.octane.integrations.services.pullrequestsandbranches.github.pojo.PullRequest> list = com.hp.octane.integrations.services.pullrequestsandbranches.github.JsonConverter.convertCollection(json, com.hp.octane.integrations.services.pullrequestsandbranches.github.pojo.PullRequest.class);
        Assert.assertEquals(30, list.size());
    }

    public String readResourceAsString(String resourceName) throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(resourceName);
        StringBuilder textBuilder = new StringBuilder();

        Reader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName(StandardCharsets.UTF_8.name())));
        int c = 0;
        while ((c = reader.read()) != -1) {
            textBuilder.append((char) c);
        }
        return textBuilder.toString();
    }
}
