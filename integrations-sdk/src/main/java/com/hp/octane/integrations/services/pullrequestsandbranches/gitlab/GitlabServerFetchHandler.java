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
package com.hp.octane.integrations.services.pullrequestsandbranches.gitlab;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.scm.Branch;
import com.hp.octane.integrations.dto.scm.PullRequest;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.dto.scm.SCMRepositoryLinks;
import com.hp.octane.integrations.dto.scm.SCMType;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.BranchFetchParameters;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.CommitUserIdPicker;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.FetchHandler;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.FetchUtils;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.PullRequestFetchParameters;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.RepoTemplates;
import com.hp.octane.integrations.services.pullrequestsandbranches.gitlab.pojo.Repository;
import com.hp.octane.integrations.services.pullrequestsandbranches.rest.authentication.AuthenticationStrategy;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AbstractUser;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GitlabServerFetchHandler extends FetchHandler {

    protected static final DTOFactory dtoFactory = DTOFactory.getInstance();

    private final String secret;
    private GitLabApi gitLabApi;
    private Project gitLabProject;
    public GitlabServerFetchHandler(AuthenticationStrategy authenticationStrategy, String token) {
        super(authenticationStrategy);
        this.secret = token;
    }

    private void initGitlab(String baseUrl) {
        if (baseUrl.endsWith(".git")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 4);
        }
        String[] split = baseUrl.split("//");

        String path = split[1];
        int i = path.indexOf('/');
        StringBuffer urlSB = new StringBuffer();
        urlSB.append(split[0]).append("//").append(path, 0, i);

        StringBuffer pathSB = new StringBuffer();
        pathSB.append(path, i + 1, path.length());

        String GLURL = urlSB.toString();
        this.gitLabApi = new GitLabApi(GitLabApi.ApiVersion.V4, GLURL, this.secret);
        try {
            this.gitLabProject = gitLabApi.getProjectApi().getProject(pathSB.toString());
        } catch (GitLabApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<PullRequest> fetchPullRequests(PullRequestFetchParameters parameters, CommitUserIdPicker commitUserIdPicker,
            Consumer<String> logger) {
        List<com.hp.octane.integrations.dto.scm.PullRequest> result = new ArrayList<>();

        List<MergeRequest> mergeRequests;
        initGitlab(parameters.getRepoUrl());
        try {
            mergeRequests = this.gitLabApi.getMergeRequestApi()
                    .getMergeRequests(this.gitLabProject.getId(), parameters.getPageSize()).all()
                    .stream()
                    .filter(mr -> mr.getUpdatedAt().getTime() > parameters.getMinUpdateTime())
                    .sorted(
                            Comparator.comparing(MergeRequest::getUpdatedAt)).collect(
                            Collectors.toList());

            //remove exceeding items
            while (mergeRequests.size() > parameters.getMaxPRsToFetch()) {
                mergeRequests.remove(0);
            }

            List<Pattern> sourcePatterns = FetchUtils.buildPatterns(parameters.getSourceBranchFilter());
            List<Pattern> targetPatterns = FetchUtils.buildPatterns(parameters.getTargetBranchFilter());
            List<MergeRequest> filteredMergeRequests = mergeRequests.stream()
                    .filter(mr -> FetchUtils.isBranchMatch(sourcePatterns, mr.getSourceBranch()) &&
                                  FetchUtils.isBranchMatch(targetPatterns, mr.getTargetBranch())).collect(Collectors.toList());
            logger.accept(String.format("Received %d merge-requests, while %d are matching source/target filters", mergeRequests.size(),
                            filteredMergeRequests.size()));

            printMergeRequestTitles(logger, mergeRequests, "Received merge-requests:");
            printMergeRequestTitles(logger, filteredMergeRequests, "Matching merge-requests:");

            if (!filteredMergeRequests.isEmpty()) {

                Set<String> usersWithoutMails =
                        filteredMergeRequests.stream().map(MergeRequest::getAuthor).filter(au -> au.getEmail() == null)
                                .map(AbstractUser::getUsername).collect(Collectors.toSet());
                if (!usersWithoutMails.isEmpty()) {
                    logger.accept("Note : Some users doesn't have defined public email in their profile. For such users, SCM user will contain their Username:  " +
                            usersWithoutMails);
                }

                logger.accept("Fetching commits ...");
                int counter = 0;
                for (MergeRequest mergeRequest : filteredMergeRequests) {
                    List<Commit> commits =
                            gitLabApi.getMergeRequestApi().getCommits(this.gitLabProject.getId(), mergeRequest.getIid());
                    List<com.hp.octane.integrations.dto.scm.SCMCommit> dtoCommits = new ArrayList<>();
                    commits.forEach(commit -> {
                        com.hp.octane.integrations.dto.scm.SCMCommit dtoCommit = dtoFactory.newDTO(com.hp.octane.integrations.dto.scm.SCMCommit.class)
                                .setRevId(commit.getId())
                                .setComment(commit.getMessage())
                                .setUser(getUserName(commit.getCommitterEmail(), commit.getCommitterName()))
                                .setUserEmail(commit.getCommitterEmail())
                                .setTime(commit.getTimestamp() != null ? commit.getTimestamp().getTime() : new Date().getTime())
                                .setParentRevId(Objects.isNull(commit.getParentIds())
                                                ? null
                                                : (commit.getParentIds().isEmpty() ? null : commit.getParentIds().get(0)));
                        dtoCommits.add(dtoCommit);
                    });

                    SCMRepository sourceRepository = buildScmRepository(parameters.getRepoUrl(),mergeRequest.getSourceBranch());
                    SCMRepository targetRepository = buildScmRepository(parameters.getRepoUrl(),mergeRequest.getTargetBranch());

                    String userId = getUserName(commitUserIdPicker, mergeRequest.getAuthor().getEmail(), mergeRequest.getAuthor().getName());
                    com.hp.octane.integrations.dto.scm.PullRequest dtoPullRequest = dtoFactory.newDTO(com.hp.octane.integrations.dto.scm.PullRequest.class)
                            .setId(Long.toString(mergeRequest.getIid()))
                            .setTitle(mergeRequest.getTitle())
                            .setDescription(mergeRequest.getDescription())
                            .setState(mergeRequest.getState())
                            .setCreatedTime(
                                    Objects.isNull(mergeRequest.getCreatedAt()) ? null : mergeRequest.getCreatedAt().getTime())
                            .setUpdatedTime(
                                    Objects.isNull(mergeRequest.getUpdatedAt()) ? null : mergeRequest.getUpdatedAt().getTime())
                            .setMergedTime(
                                    Objects.isNull(mergeRequest.getMergedAt()) ? null : mergeRequest.getMergedAt().getTime())
                            .setIsMerged(Objects.nonNull(mergeRequest.getMergedAt()))
                            .setAuthorName(userId)
                            .setAuthorEmail(Objects.isNull(mergeRequest.getAuthor()) ? null : mergeRequest.getAuthor().getEmail())
                            .setClosedTime(Objects.isNull(mergeRequest.getClosedAt()) ? null : mergeRequest.getClosedAt().getTime())
                            .setSelfUrl(mergeRequest.getWebUrl())
                            .setSourceRepository(sourceRepository)
                            .setTargetRepository(targetRepository)
                            .setCommits(dtoCommits);
                    result.add(dtoPullRequest);
                    if (counter > 0 && counter % 25 == 0) {
                        logger.accept("Fetching commits " + counter * 100 / filteredMergeRequests.size() + "%");
                    }
                    counter++;
                }
            }
            else {
                logger.accept("No new/updated PR is found.");
            }
            return result;

        } catch (GitLabApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void printMergeRequestTitles(Consumer<String> logConsumer, List<MergeRequest> mergeRequests, String textToPrint) {
        if (mergeRequests.isEmpty()) {
            return;
        }

        logConsumer.accept(textToPrint);
        for (MergeRequest mr : mergeRequests) {
            String prTitle = (null == mr.getTitle()) ? "<no title>" : mr.getTitle();
            logConsumer.accept(prTitle);
        }
    }

    @Override
    public List<Branch> fetchBranches(BranchFetchParameters parameters, Map<String, Long> sha2DateMapCache,
            Consumer<String> logger) throws IOException {

        initGitlab(parameters.getRepoUrl());
        List<org.gitlab4j.api.models.Branch> branches;
        try {
            branches = gitLabApi.getRepositoryApi().getBranches(this.gitLabProject.getId(), parameters.getPageSize()).all();
        } catch (GitLabApiException e) {
            throw new RuntimeException(e);
        }
        List<Pattern> filterPatterns = FetchUtils.buildPatterns(parameters.getFilter());
        List<Branch> filteredBranches = branches.stream().filter(br -> FetchUtils.isBranchMatch(filterPatterns, br.getName()))
                .map(this::convertToDTOBranch).collect(Collectors.toList());
        long outdatedTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(parameters.getActiveBranchDays());
        int fetched = 0;
        int outdated = 0;
        for (com.hp.octane.integrations.dto.scm.Branch branch : filteredBranches) {

            if (sha2DateMapCache != null && sha2DateMapCache.containsKey(branch.getLastCommitSHA())) {
                branch.setLastCommitTime(sha2DateMapCache.get(branch.getLastCommitSHA()));
                if (branch.getLastCommitTime() < outdatedTime) {
                    outdated++;
                    continue;
                }
            }
            if (fetched < parameters.getMaxBranchesToFill()) {
                branch.setPartial(false);
                fetched++;
            }


        }
        logger.accept(String.format("Found %d branches in GitLab, while %d are matching filters", branches.size(),
                filteredBranches.size()));
        if (!filteredBranches.isEmpty()) {
            logger.accept("Fetching branches is done");
        } else {
            logger.accept("No new/updated  branch is found.");
        }


        logger.accept(String.format("Fetching branches is done, fetched %s, skipped as outdated %s", fetched, outdated));
        return filteredBranches;


    }

    private com.hp.octane.integrations.dto.scm.Branch convertToDTOBranch(
            org.gitlab4j.api.models.Branch branch) {
        return DTOFactory.getInstance().newDTO(com.hp.octane.integrations.dto.scm.Branch.class)
                .setName(branch.getName())
                .setLastCommitUrl(branch.getCommit().getUrl())
                .setLastCommitSHA(branch.getCommit().getId())
                .setLastCommitTime(branch.getCommit().getCommittedDate().getTime())
                .setLastCommiterName(branch.getCommit().getCommitterName())
                .setLastCommiterEmail(branch.getCommit().getCommitterEmail())
                .setIsMerged(branch.getMerged())
                .setPartial(true);
    }

    @Override
    public String getRepoApiPath(String repoHttpCloneUrl) {
        //https://hmf.gitlab.otxlab.net/adm-sdp/something/somethingElse => https://hmf.gitlab.otxlab.net/api/v4/projects/adm-sdp/something%2FsomethingElse
        try {

            if (repoHttpCloneUrl.endsWith(".git")) {
                repoHttpCloneUrl = repoHttpCloneUrl.substring(0, repoHttpCloneUrl.length() - 4);
            }


            if (repoHttpCloneUrl.contains("//")) {
                List<String> list = Arrays.asList(repoHttpCloneUrl.split("//"));
                String rest = list.get(1);
                int i = rest.indexOf('/');
                String encoded = rest.substring(i + 1).replace("/", "%2F");
                StringBuffer sb = new StringBuffer();
                sb.append(list.get(0)).append("//").append(rest, 0, i).append("/api/v4/projects/").append(encoded);
                return sb.toString();
            } else {
                throw new Exception();
            }


        } catch (Exception e) {
            throw new IllegalArgumentException("Unexpected format for gitlab server repository URL : " + repoHttpCloneUrl);
        }
    }


    @Override
    protected String parseRequestError(OctaneResponse response) {
        return JsonConverter.getErrorMessage(response.getBody());
    }

    @Override
    public SCMRepositoryLinks parseSCMRepositoryLinks(String responseBody) throws JsonProcessingException {
        Repository repo = JsonConverter.convert(responseBody, Repository.class);
        SCMRepositoryLinks links = dtoFactory.newDTO(SCMRepositoryLinks.class).setHttpUrl(repo.getHttp_url_to_repo()).setSshUrl(repo.getSsh_url_to_repo());
        return links;
    }

    @Override
    public RepoTemplates buildRepoTemplates(String repoApiBaseUrl) {
        String selfUrl = repoApiBaseUrl.substring(0, repoApiBaseUrl.length() - 4);//remove .git at the end
        RepoTemplates repoTemplates = new RepoTemplates();
        repoTemplates.setDiffTemplate(selfUrl + "/commit/{revision}#{filePath}");
        repoTemplates.setSourceViewTemplate(selfUrl + "/blob/{revision}/{filePath}");
        repoTemplates.setBranchFileTemplate(selfUrl + "/tree/{branchName}/{filePath}");
        return repoTemplates;
    }

    private SCMRepository buildScmRepository(String repoUrl, String branch) {
        return dtoFactory.newDTO(SCMRepository.class)
                .setUrl(repoUrl)
                .setBranch(branch)
                .setType(SCMType.GIT);
    }

}
