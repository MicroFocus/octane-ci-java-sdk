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
package com.hp.octane.integrations.services.pullrequestsandbranches.bitbucketserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.dto.scm.SCMRepositoryLinks;
import com.hp.octane.integrations.dto.scm.SCMType;
import com.hp.octane.integrations.services.pullrequestsandbranches.bitbucketserver.pojo.*;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.*;
import com.hp.octane.integrations.services.pullrequestsandbranches.rest.authentication.AuthenticationStrategy;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BitbucketServerFetchHandler extends FetchHandler {

    public BitbucketServerFetchHandler(AuthenticationStrategy authenticationStrategy) {
        super(authenticationStrategy);
    }

    @Override
    public List<com.hp.octane.integrations.dto.scm.PullRequest> fetchPullRequests(PullRequestFetchParameters parameters, CommitUserIdPicker commitUserIdPicker, Consumer<String> logConsumer) throws IOException {

        List<com.hp.octane.integrations.dto.scm.PullRequest> result = new ArrayList<>();
        String baseUrl = getRepoApiPath(parameters.getRepoUrl());
        logConsumer.accept("BitbucketServerRestHandler, Base url : " + baseUrl);
        SCMRepositoryLinks links = pingRepository(baseUrl, logConsumer);
        parameters.setRepoUrlSsh(links.getSshUrl());
        if(parameters.isUseSSHFormat()){
            logConsumer.accept("Repo ssh format url : " + parameters.getRepoUrlSsh());
        }

        String pullRequestsUrl = baseUrl + "/pull-requests?state=ALL";
        logConsumer.accept("Pull requests url : " + pullRequestsUrl);

        List<PullRequest> pullRequests = getPagedEntities(pullRequestsUrl, PullRequest.class, parameters.getPageSize(), parameters.getMaxPRsToFetch(), parameters.getMinUpdateTime());
        List<Pattern> sourcePatterns = FetchUtils.buildPatterns(parameters.getSourceBranchFilter());
        List<Pattern> targetPatterns = FetchUtils.buildPatterns(parameters.getTargetBranchFilter());

        List<PullRequest> filteredPullRequests = pullRequests.stream()
                .filter(pr -> FetchUtils.isBranchMatch(sourcePatterns, pr.getFromRef().getDisplayId()) && FetchUtils.isBranchMatch(targetPatterns, pr.getToRef().getDisplayId()))
                .collect(Collectors.toList());
        logConsumer.accept(String.format("Received %d pull-requests, while %d are matching source/target filters", pullRequests.size(), filteredPullRequests.size()));

        if (!filteredPullRequests.isEmpty()) {
            logConsumer.accept("Fetching commits ...");
            int counter = 0;
            for (PullRequest pr : filteredPullRequests) {
                String url = baseUrl + "/pull-requests/" + pr.getId() + "/commits";
                List<Commit> commits = Collections.emptyList();
                try {
                    commits = getPagedEntities(url, Commit.class, parameters.getPageSize(), parameters.getMaxCommitsToFetch(), parameters.getMinUpdateTime());
                } catch (Exception e) {
                    //this issue was raised by customer : after merging PR with squash, branch was deleted and get commit of PR - returned with 404
                    //Request to '.../pull-requests/259/commits?&limit=30&start=0' is ended with result 404 : Commit 'a44a0c2fc' does not exist in repository '...'.
                    logConsumer.accept(String.format("Failed to fetch commits for PR %s : %s", pr.getId(), e.getMessage()));
                }
                List<com.hp.octane.integrations.dto.scm.SCMCommit> dtoCommits = new ArrayList<>();
                for (Commit commit : commits) {
                    com.hp.octane.integrations.dto.scm.SCMCommit dtoCommit = dtoFactory.newDTO(com.hp.octane.integrations.dto.scm.SCMCommit.class)
                            .setRevId(commit.getId())
                            .setComment(commit.getMessage())
                            .setUser(getUserName(commit.getCommitter().getEmailAddress(), commit.getCommitter().getName()))
                            .setUserEmail(commit.getCommitter().getEmailAddress())
                            .setTime(commit.getCommitterTimestamp())
                            .setParentRevId(commit.getParents().get(0).getId());
                    dtoCommits.add(dtoCommit);
                    }


                SCMRepository sourceRepository = buildScmRepository(parameters.isUseSSHFormat(), pr.getFromRef());
                SCMRepository targetRepository = buildScmRepository(parameters.isUseSSHFormat(), pr.getToRef());

                boolean isMerged = PullRequest.MERGED_STATE.equals(pr.getState());
                String userId = getUserName(commitUserIdPicker, pr.getAuthor().getUser().getEmailAddress(), pr.getAuthor().getUser().getName());
                com.hp.octane.integrations.dto.scm.PullRequest dtoPullRequest = dtoFactory.newDTO(com.hp.octane.integrations.dto.scm.PullRequest.class)
                        .setId(pr.getId())
                        .setTitle(pr.getTitle())
                        .setDescription(pr.getDescription())
                        .setState(pr.getState())
                        .setCreatedTime(pr.getCreatedDate())
                        .setUpdatedTime(pr.getUpdatedTime())
                        .setAuthorName(userId)
                        .setAuthorEmail(pr.getAuthor().getUser().getEmailAddress())
                        .setClosedTime(pr.getClosedDate())
                        .setSelfUrl(pr.getLinks().getSelf().get(0).getHref())
                        .setSourceRepository(sourceRepository)
                        .setTargetRepository(targetRepository)
                        .setCommits(dtoCommits)
                        .setMergedTime(isMerged ? pr.getClosedDate() : null)
                        .setIsMerged(isMerged);
                result.add(dtoPullRequest);

                if (counter > 0 && counter % 40 == 0) {
                    logConsumer.accept("Fetching commits " + counter * 100 / filteredPullRequests.size() + "%");
                }
                counter++;
            }
            logConsumer.accept("Fetching commits is done");
            logConsumer.accept("Pull requests are ready");
        } else {
            logConsumer.accept("No new/updated PR is found.");
        }
        return result;
    }

    @Override
    public List<com.hp.octane.integrations.dto.scm.Branch> fetchBranches(BranchFetchParameters parameters, Map<String, Long> sha2DateMapCache, Consumer<String> logConsumer) throws IOException {
        String baseUrl = getRepoApiPath(parameters.getRepoUrl());

        String branchesUrl = baseUrl + "/branches?&details=true&&orderBy=MODIFICATION";
        logConsumer.accept("Branches url : " + branchesUrl);

        List<Branch> branches = getPagedEntities(branchesUrl, Branch.class, parameters.getPageSize(), Integer.MAX_VALUE, null);
        List<Pattern> searchPatterns = FetchUtils.buildPatterns(parameters.getFilter());

        List<com.hp.octane.integrations.dto.scm.Branch> filteredBranches = branches.stream()
                .filter(br -> FetchUtils.isBranchMatch(searchPatterns, br.getDisplayId()))
                .map((this::convertToDTOBranch))
                .collect(Collectors.toList());
        logConsumer.accept(String.format("Found %d branches in Bitbucket, while %d are matching filter", branches.size(), filteredBranches.size()));

        if (!filteredBranches.isEmpty()) {
            logConsumer.accept("Fetching branches is done");
        } else {
            logConsumer.accept("No new/updated  branch is found.");
        }

        return filteredBranches;
    }

    private com.hp.octane.integrations.dto.scm.Branch convertToDTOBranch(Branch branch) {
        BranchMetadataLatestCommit latest = branch.getMetadata().getLatestCommit();
        boolean isMerged = branch.getIsDefault() ? true : (branch.getMetadata().getAheadBehind().getAhead() == 0);
        com.hp.octane.integrations.dto.scm.Branch branchDTO = DTOFactory.getInstance().newDTO(com.hp.octane.integrations.dto.scm.Branch.class)
                .setName(branch.getDisplayId())
                .setLastCommitSHA(latest.getId())
                .setLastCommitTime(latest.getCommitterTimestamp())
                .setLastCommiterName(latest.getCommitter().getName())
                .setLastCommiterEmail(latest.getCommitter().getEmailAddress())
                .setIsMerged(isMerged);
        return branchDTO;
    }

    private SCMRepository buildScmRepository(boolean useSSHFormat, Ref ref) {
        Stream<Link> links = ref.getRepository().getLinks().getClone().stream();
        Optional<Link> optLink = useSSHFormat ? links.filter(l -> l.getName().equalsIgnoreCase("ssh")).findFirst() :
                links.filter(l -> !l.getName().equalsIgnoreCase("ssh")).findFirst();
        String url = optLink.isPresent() ? optLink.get().getHref() : ref.getRepository().getLinks().getClone().get(0).getHref();
        return dtoFactory.newDTO(SCMRepository.class)
                .setUrl(url)
                .setBranch(ref.getDisplayId())
                .setType(SCMType.GIT);
    }

    private <T extends Entity & SupportUpdatedTime> List<T> getPagedEntities(String url, Class<T> entityType, int pageSize, int maxTotal, Long minUpdateTime) {
        try {
            //https://developer.atlassian.com/server/confluence/pagination-in-the-rest-api/
            List<T> result = new ArrayList<>();
            boolean finished;
            int limit = pageSize;
            int start = 0;
            do {
                String myUrl = url + (url.contains("?") ? "" : "?") + String.format("&limit=%d&start=%d", limit, start);
                OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class).setUrl(myUrl).setMethod(HttpMethod.GET);
                OctaneResponse response = restClient.executeRequest(request);
                if (response.getStatus() != HttpStatus.SC_OK) {
                    throw new RuntimeException(String.format("Request to '%s' is ended with result %d : %s", myUrl, response.getStatus(), JsonConverter.getErrorMessage(response.getBody())));
                }
                EntityCollection<T> collection = JsonConverter.convertCollection(response.getBody(), entityType);
                result.addAll(collection.getValues());
                finished = collection.isLastPage() || result.size() > maxTotal;
                limit = collection.getLimit();
                start = collection.getStart() + collection.getLimit();

                //remove outdated items
                if (minUpdateTime != null) {
                    for (int i = result.size() - 1; i >= 0; i--) {
                        if (result.get(i).getUpdatedTime() <= minUpdateTime) {
                            result.remove(i);
                            finished = true;
                        } else {
                            break;
                        }
                    }
                }
            } while (!finished);

            //remove exceeded items
            while (result.size() > maxTotal) {
                result.remove(result.size() - 1);
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to getPagedEntities : " + e.getMessage(), e);
        }
    }

    @Override
    public String getRepoApiPath(String repoHttpCloneUrl) {
        validateHttpCloneUrl(repoHttpCloneUrl);

        //http://localhost:8990/scm/proj/rep1.git   =>http://localhost:8990/rest/api/1.0/projects/proj/repos/rep1
        //http://localhost:8990/scm/~admin/rep1.git =>http://localhost:8990/rest/api/1.0/users/admin/repos/rep1
        try {
            List<String> parts = Arrays.asList(repoHttpCloneUrl.trim().split("/"));

            int scmIndex = repoHttpCloneUrl.toLowerCase().indexOf("/scm/");
            StringBuilder sb = new StringBuilder();

            sb.append(repoHttpCloneUrl, 0, scmIndex);
            sb.append("/rest/api/1.0");

            //add project or username
            String projOrUserPart = parts.get(parts.size() - 2);
            if (projOrUserPart.startsWith("~")) {
                //set /users/userName
                sb.append("/users/");
                sb.append(projOrUserPart.substring(1));//remove ~
            } else {
                //set /projects/projName
                sb.append("/projects/");
                sb.append(projOrUserPart);
            }

            //add repo name without .git
            String repoPart = parts.get(parts.size() - 1);
            if (repoPart.toLowerCase().endsWith(".git")) {
                repoPart = repoPart.substring(0, repoPart.length() - 4);//remove ".git"
            }
            sb.append("/repos/");
            sb.append(repoPart);

            return sb.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unexpected bitbucket server repository URL : " + repoHttpCloneUrl + ". Expected formats : http(s)://<bitbucket_server>:<port>/scm/<project_name>/<repository_name>.git" +
                    " or  http(s)://<bitbucket_server>:<port>/scm/~<user_name>/<repository_name>.git");
        }
    }

    private String getSelfUrl(String repoHttpCloneUrl) {
        //http://myd-hvm02624.swinfra.net:7990/scm/tes/simple-tests.git=>http://myd-hvm02624.swinfra.net:7990/projects/TES/repos/simple-tests
        //http://myd-hvm02624.swinfra.net:7990/scm/~admin/simple-tests-forked.git=>http://myd-hvm02624.swinfra.net:7990/users/admin/repos/simple-tests-forked
        List<String> parts = Arrays.asList(repoHttpCloneUrl.trim().split("/"));

        int scmIndex = repoHttpCloneUrl.toLowerCase().indexOf("/scm/");
        StringBuilder sb = new StringBuilder();
        sb.append(repoHttpCloneUrl, 0, scmIndex);

        //add project or username
        String projOrUserPart = parts.get(parts.size() - 2);
        if (projOrUserPart.startsWith("~")) {
            //set /users/userName
            sb.append("/users/");
            sb.append(projOrUserPart.substring(1));//remove ~
        } else {
            //set /projects/projName
            sb.append("/projects/");
            sb.append(projOrUserPart);
        }

        //add repo name without .git
        String repoPart = parts.get(parts.size() - 1);
        if (repoPart.toLowerCase().endsWith(".git")) {
            repoPart = repoPart.substring(0, repoPart.length() - 4);//remove ".git"
        }
        sb.append("/repos/");
        sb.append(repoPart);


        return sb.toString();
    }

    @Override
    protected String parseRequestError(OctaneResponse response) {
        return JsonConverter.getErrorMessage(response.getBody());
    }

    @Override
    public SCMRepositoryLinks parseSCMRepositoryLinks(String responseBody) throws JsonProcessingException {
        Repository repo = JsonConverter.convert(responseBody, Repository.class);

        SCMRepositoryLinks links = dtoFactory.newDTO(SCMRepositoryLinks.class);
        repo.getLinks().getClone().forEach(l -> {
            if ("ssh".equalsIgnoreCase(l.getName())) {
                links.setSshUrl(l.getHref());
            } else {
                links.setHttpUrl(l.getHref());
            }
        });
        return links;
    }

    @Override
    public RepoTemplates buildRepoTemplates(String repoHttpCloneUrl) {
        String selfUrl = getSelfUrl((repoHttpCloneUrl));
        RepoTemplates repoTemplates = new RepoTemplates();
        repoTemplates.setDiffTemplate(selfUrl + "/commits/{revision}#{filePath}");
        repoTemplates.setSourceViewTemplate(selfUrl + "/browse/{filePath}?until={revision}&untilPath={filePath}");
        repoTemplates.setBranchFileTemplate(selfUrl + "/browse/{filePath}?at={branchName}");
        return repoTemplates;
    }
}
