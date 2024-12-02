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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hp.octane.integrations.dto.scm.SCMRepositoryLinks;
import com.hp.octane.integrations.services.pullrequestsandbranches.bitbucketserver.BitbucketServerFetchHandler;
import com.hp.octane.integrations.services.pullrequestsandbranches.github.GithubCloudFetchHandler;
import com.hp.octane.integrations.services.pullrequestsandbranches.github.GithubServerFetchHandler;
import com.hp.octane.integrations.services.pullrequestsandbranches.rest.authentication.NoCredentialsStrategy;
import org.junit.Assert;
import org.junit.Test;

public class FetchHandlerTests {

    @Test
    public void githubCloudTest() {
        GithubCloudFetchHandler handler = new GithubCloudFetchHandler(new NoCredentialsStrategy());

        String result = handler.getRepoApiPath("https://github.com/jenkinsci/hpe-application-automation-tools-plugin.git");
        Assert.assertEquals("getRepoApiPath for https failed", "https://api.github.com/repos/jenkinsci/hpe-application-automation-tools-plugin", result);

        result = handler.getApiPath("https://github.com/jenkinsci/hpe-application-automation-tools-plugin.git");
        Assert.assertEquals("getApiPath for https failed", "https://api.github.com", result);
    }

    @Test
    public void githubServerTest() {
        GithubServerFetchHandler handler = new GithubServerFetchHandler(new NoCredentialsStrategy());

        String result = handler.getRepoApiPath("https://github.houston.softwaregrp.net/MQM/mqm.git");
        Assert.assertEquals("getRepoApiPath for https failed", "https://github.houston.softwaregrp.net/api/v3/repos/MQM/mqm", result);


        result = handler.getApiPath("https://github.houston.softwaregrp.net/MQM/mqm.git");
        Assert.assertEquals("getApiPath for https failed", "https://github.houston.softwaregrp.net/api/v3", result);
    }

    @Test
    public void bitbucketServerTest() {
        BitbucketServerFetchHandler handler = new BitbucketServerFetchHandler(new NoCredentialsStrategy());

        String result = handler.getRepoApiPath("http://localhost:8990/scm/proj/rep1.git");
        Assert.assertEquals("getRepoApiPath for https failed", "http://localhost:8990/rest/api/1.0/projects/proj/repos/rep1", result);

        result = handler.getRepoApiPath("http://localhost:8990/scm/~admin/rep1.git");
        Assert.assertEquals("getRepoApiPath for https failed", "http://localhost:8990/rest/api/1.0/users/admin/repos/rep1", result);
    }

    @Test
    public void bitbucketParseLinks() throws JsonProcessingException {
        String body = "{\"slug\":\"simple-tests\",\"id\":1,\"name\":\"simple-tests\",\"hierarchyId\":\"3652cee12ecd25817b22\",\"scmId\":\"git\",\"state\":\"AVAILABLE\",\"statusMessage\":\"Available\",\"forkable\":true,\"project\":{\"key\":\"TES\",\"id\":2,\"name\":\"tests\",\"public\":false,\"type\":\"NORMAL\",\"links\":{\"self\":[{\"href\":\"http://myd-hvm02624.swinfra.net:7990/projects/TES\"}]}},\"public\":true,\"links\":{\"clone\":[{\"href\":\"ssh://git@myd-hvm02624.swinfra.net:7999/tes/simple-tests.git\",\"name\":\"ssh\"},{\"href\":\"http://myd-hvm02624.swinfra.net:7990/scm/tes/simple-tests.git\",\"name\":\"http\"}],\"self\":[{\"href\":\"http://myd-hvm02624.swinfra.net:7990/projects/TES/repos/simple-tests/browse\"}]}}";
        BitbucketServerFetchHandler handler = new BitbucketServerFetchHandler(new NoCredentialsStrategy());
        SCMRepositoryLinks links = handler.parseSCMRepositoryLinks(body);
        Assert.assertEquals("ssh://git@myd-hvm02624.swinfra.net:7999/tes/simple-tests.git", links.getSshUrl());
        Assert.assertEquals("http://myd-hvm02624.swinfra.net:7990/scm/tes/simple-tests.git", links.getHttpUrl());
    }

    @Test
    public void githubServerParseLinks() throws JsonProcessingException {
        String body = "{\"id\":237845737,\"node_id\":\"MDEwOlJlcG9zaXRvcnkyMzc4NDU3Mzc=\",\"name\":\"trial\",\"full_name\":\"radislavB/trial\",\"private\":false,\"owner\":{\"login\":\"radislavB\",\"id\":20180777,\"node_id\":\"MDQ6VXNlcjIwMTgwNzc3\",\"avatar_url\":\"https://avatars.githubusercontent.com/u/20180777?v=4\",\"gravatar_id\":\"\",\"url\":\"https://api.github.com/users/radislavB\",\"html_url\":\"https://github.com/radislavB\",\"followers_url\":\"https://api.github.com/users/radislavB/followers\",\"following_url\":\"https://api.github.com/users/radislavB/following{/other_user}\",\"gists_url\":\"https://api.github.com/users/radislavB/gists{/gist_id}\",\"starred_url\":\"https://api.github.com/users/radislavB/starred{/owner}{/repo}\",\"subscriptions_url\":\"https://api.github.com/users/radislavB/subscriptions\",\"organizations_url\":\"https://api.github.com/users/radislavB/orgs\",\"repos_url\":\"https://api.github.com/users/radislavB/repos\",\"events_url\":\"https://api.github.com/users/radislavB/events{/privacy}\",\"received_events_url\":\"https://api.github.com/users/radislavB/received_events\",\"type\":\"User\",\"site_admin\":false},\"html_url\":\"https://github.com/radislavB/trial\",\"description\":\"trial\",\"fork\":false,\"url\":\"https://api.github.com/repos/radislavB/trial\",\"forks_url\":\"https://api.github.com/repos/radislavB/trial/forks\",\"keys_url\":\"https://api.github.com/repos/radislavB/trial/keys{/key_id}\",\"collaborators_url\":\"https://api.github.com/repos/radislavB/trial/collaborators{/collaborator}\",\"teams_url\":\"https://api.github.com/repos/radislavB/trial/teams\",\"hooks_url\":\"https://api.github.com/repos/radislavB/trial/hooks\",\"issue_events_url\":\"https://api.github.com/repos/radislavB/trial/issues/events{/number}\",\"events_url\":\"https://api.github.com/repos/radislavB/trial/events\",\"assignees_url\":\"https://api.github.com/repos/radislavB/trial/assignees{/user}\",\"branches_url\":\"https://api.github.com/repos/radislavB/trial/branches{/branch}\",\"tags_url\":\"https://api.github.com/repos/radislavB/trial/tags\",\"blobs_url\":\"https://api.github.com/repos/radislavB/trial/git/blobs{/sha}\",\"git_tags_url\":\"https://api.github.com/repos/radislavB/trial/git/tags{/sha}\",\"git_refs_url\":\"https://api.github.com/repos/radislavB/trial/git/refs{/sha}\",\"trees_url\":\"https://api.github.com/repos/radislavB/trial/git/trees{/sha}\",\"statuses_url\":\"https://api.github.com/repos/radislavB/trial/statuses/{sha}\",\"languages_url\":\"https://api.github.com/repos/radislavB/trial/languages\",\"stargazers_url\":\"https://api.github.com/repos/radislavB/trial/stargazers\",\"contributors_url\":\"https://api.github.com/repos/radislavB/trial/contributors\",\"subscribers_url\":\"https://api.github.com/repos/radislavB/trial/subscribers\",\"subscription_url\":\"https://api.github.com/repos/radislavB/trial/subscription\",\"commits_url\":\"https://api.github.com/repos/radislavB/trial/commits{/sha}\",\"git_commits_url\":\"https://api.github.com/repos/radislavB/trial/git/commits{/sha}\",\"comments_url\":\"https://api.github.com/repos/radislavB/trial/comments{/number}\",\"issue_comment_url\":\"https://api.github.com/repos/radislavB/trial/issues/comments{/number}\",\"contents_url\":\"https://api.github.com/repos/radislavB/trial/contents/{+path}\",\"compare_url\":\"https://api.github.com/repos/radislavB/trial/compare/{base}...{head}\",\"merges_url\":\"https://api.github.com/repos/radislavB/trial/merges\",\"archive_url\":\"https://api.github.com/repos/radislavB/trial/{archive_format}{/ref}\",\"downloads_url\":\"https://api.github.com/repos/radislavB/trial/downloads\",\"issues_url\":\"https://api.github.com/repos/radislavB/trial/issues{/number}\",\"pulls_url\":\"https://api.github.com/repos/radislavB/trial/pulls{/number}\",\"milestones_url\":\"https://api.github.com/repos/radislavB/trial/milestones{/number}\",\"notifications_url\":\"https://api.github.com/repos/radislavB/trial/notifications{?since,all,participating}\",\"labels_url\":\"https://api.github.com/repos/radislavB/trial/labels{/name}\",\"releases_url\":\"https://api.github.com/repos/radislavB/trial/releases{/id}\",\"deployments_url\":\"https://api.github.com/repos/radislavB/trial/deployments\",\"created_at\":\"2020-02-02T22:21:33Z\",\"updated_at\":\"2021-03-04T12:02:08Z\",\"pushed_at\":\"2021-03-04T12:02:06Z\",\"git_url\":\"git://github.com/radislavB/trial.git\",\"ssh_url\":\"git@github.com:radislavB/trial.git\",\"clone_url\":\"https://github.com/radislavB/trial.git\",\"svn_url\":\"https://github.com/radislavB/trial\",\"homepage\":null,\"size\":45,\"stargazers_count\":0,\"watchers_count\":0,\"language\":null,\"has_issues\":true,\"has_projects\":true,\"has_downloads\":true,\"has_wiki\":true,\"has_pages\":false,\"forks_count\":0,\"mirror_url\":null,\"archived\":false,\"disabled\":false,\"open_issues_count\":4,\"license\":null,\"forks\":0,\"open_issues\":4,\"watchers\":0,\"default_branch\":\"master\",\"permissions\":{\"admin\":true,\"push\":true,\"pull\":true},\"temp_clone_token\":\"\",\"allow_squash_merge\":true,\"allow_merge_commit\":true,\"allow_rebase_merge\":true,\"delete_branch_on_merge\":false,\"network_count\":0,\"subscribers_count\":1}";
        GithubServerFetchHandler handler = new GithubServerFetchHandler(new NoCredentialsStrategy());
        SCMRepositoryLinks links = handler.parseSCMRepositoryLinks(body);
        Assert.assertEquals("git@github.com:radislavB/trial.git", links.getSshUrl());
        Assert.assertEquals("https://github.com/radislavB/trial.git", links.getHttpUrl());
    }
}
