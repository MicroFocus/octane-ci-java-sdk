
package com.hp.octane.integrations.services.pullrequestsandbranches.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.dto.scm.SCMRepositoryLinks;
import com.hp.octane.integrations.dto.scm.SCMType;
import com.hp.octane.integrations.exceptions.ResourceNotFoundException;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.*;
import com.hp.octane.integrations.services.pullrequestsandbranches.github.pojo.*;
import com.hp.octane.integrations.services.pullrequestsandbranches.rest.authentication.AuthenticationStrategy;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class GithubV3FetchHandler extends FetchHandler {

    public static final String CLOUD_SERVICE_PREFIX = "https://github.com/";

    public GithubV3FetchHandler(AuthenticationStrategy authenticationStrategy) {
        super(authenticationStrategy);
    }

    private static final long NO_MIN_UPDATE_TIME = 0;

    @Override
    protected String parseRequestError(OctaneResponse response) {
        return JsonConverter.getErrorMessage(response.getBody());
    }

    @Override
    public List<com.hp.octane.integrations.dto.scm.Branch> fetchBranches(BranchFetchParameters fp, Map<String, Long> sha2DateMapCache, Consumer<String> logConsumer) throws IOException {
        String baseUrl = getRepoApiPath(fp.getRepoUrl());
        String apiUrl = getApiPath(fp.getRepoUrl());

        RateLimitationInfo rateLimitationInfo = getRateLimitationInfo(apiUrl, logConsumer);

        String branchesUrl = baseUrl + "/branches";
        logConsumer.accept("Branches url : " + branchesUrl);
        List<Branch> branches = getPagedEntities(branchesUrl, Branch.class, fp.getPageSize(), Integer.MAX_VALUE, NO_MIN_UPDATE_TIME);
        List<Pattern> filterPatterns = FetchUtils.buildPatterns(fp.getFilter());
        List<com.hp.octane.integrations.dto.scm.Branch> filteredBranches = branches.stream()
                .filter(br -> FetchUtils.isBranchMatch(filterPatterns, br.getName()))
                .map((br -> convertToDTOBranch(br)))
                .collect(Collectors.toList());
        logConsumer.accept(String.format("Found %d branches in Github, while %d are matching filters", branches.size(), filteredBranches.size()));

        //find repo to know default branch
        Repo repo = getEntity(baseUrl, Repo.class);

        //fill branches
        long outdatedTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(fp.getActiveBranchDays());
        int fetched = 0;
        int rateLimited = 0;
        int outdated = 0;
        int counter = 0;
        int notificationNumber = Math.max(20, filteredBranches.size() * 2 / 100);//every x branches - we will print message to console, x max of (2% of branches , 20 branches)
        for (com.hp.octane.integrations.dto.scm.Branch branch : filteredBranches) {
            counter++;

            if (sha2DateMapCache != null && sha2DateMapCache.containsKey(branch.getLastCommitSHA())) {
                branch.setLastCommitTime(sha2DateMapCache.get(branch.getLastCommitSHA()));
                if (branch.getLastCommitTime() < outdatedTime) {
                    outdated++;
                    continue;
                }
            }

            if ((rateLimitationInfo == null || rateLimitationInfo.getRemaining() > 2) && fetched < fp.getMaxBranchesToFill()) {
                String urlCompareBranchUrl = String.format("%s/compare/%s...%s", baseUrl, repo.getDefault_branch(), branch.getLastCommitSHA());
                Compare compare = getEntity(urlCompareBranchUrl, Compare.class);
                Commit lastCommit = getEntity(branch.getLastCommitUrl(), Commit.class, rateLimitationInfo);
                String lastCommitTime = null;
                try {
                    lastCommitTime = lastCommit.getCommit().getCommitter().getDate();
                } catch (Exception e) {
                    logConsumer.accept("branch doesn't have last commit  data information : " + branch.getName());

                }
                branch
                        .setLastCommitTime(FetchUtils.convertISO8601DateStringToLong(lastCommitTime))
                        .setLastCommiterName(lastCommit.getCommit().getAuthor().getName())
                        .setLastCommiterEmail(lastCommit.getCommit().getAuthor().getEmail())
                        .setIsMerged(compare.getAhead_by() == 0)
                        .setPartial(false);
                fetched++;
            } else {
                if (rateLimited == 0) {
                    logConsumer.accept(String.format("Skipping fetching because of rate limit. First skipped branch '%s'.)", branch.getName()));
                }
                rateLimited++;
            }

            if (rateLimited == 0 && counter > 0 && counter % notificationNumber == 0) {
                logConsumer.accept("Fetching branch information " + counter * 100 / filteredBranches.size() + "%");
            }
        }

        getRateLimitationInfo(apiUrl, logConsumer);
        logConsumer.accept(String.format("Fetching branches is done, fetched %s, skipped as outdated %s, skipped because of max limit/rate limit %s", fetched, outdated, rateLimited));
        return filteredBranches;
    }

    private com.hp.octane.integrations.dto.scm.Branch convertToDTOBranch(Branch branch) {
        return DTOFactory.getInstance().newDTO(com.hp.octane.integrations.dto.scm.Branch.class)
                .setName(branch.getName())
                .setLastCommitSHA(branch.getCommit().getSha())
                .setLastCommitUrl(branch.getCommit().getUrl())
                .setPartial(true);
    }

    public abstract String getApiPath(String repoHttpCloneUrl);

    @Override
    public List<com.hp.octane.integrations.dto.scm.PullRequest> fetchPullRequests(PullRequestFetchParameters parameters, CommitUserIdPicker commitUserIdPicker, Consumer<String> logConsumer) throws IOException {

        List<com.hp.octane.integrations.dto.scm.PullRequest> result = new ArrayList<>();
        String baseUrl = getRepoApiPath(parameters.getRepoUrl());
        String apiUrl = getApiPath(parameters.getRepoUrl());
        logConsumer.accept(this.getClass().getSimpleName() + " handler, Base url : " + baseUrl);
        SCMRepositoryLinks links = pingRepository(baseUrl, logConsumer);
        parameters.setRepoUrlSsh(links.getSshUrl());
        if (parameters.isUseSSHFormat()) {
            logConsumer.accept("Repo ssh format url : " + parameters.getRepoUrlSsh());
        }

        getRateLimitationInfo(apiUrl, logConsumer);

        String pullRequestsUrl = baseUrl + "/pulls?state=all";
        logConsumer.accept("Pull requests url : " + pullRequestsUrl);

        //prs are returned in asc order by id , therefore we need to get all before filtering , therefore page size equals to max total
        List<PullRequest> pullRequests = getPagedEntities(pullRequestsUrl, PullRequest.class, parameters.getMaxPRsToFetch(), parameters.getMaxPRsToFetch(), parameters.getMinUpdateTime());
        List<Pattern> sourcePatterns = FetchUtils.buildPatterns(parameters.getSourceBranchFilter());
        List<Pattern> targetPatterns = FetchUtils.buildPatterns(parameters.getTargetBranchFilter());

        List<PullRequest> filteredPullRequests = pullRequests.stream()
                .filter(pr -> FetchUtils.isBranchMatch(sourcePatterns, pr.getHead().getRef()) && FetchUtils.isBranchMatch(targetPatterns, pr.getBase().getRef()))
                .collect(Collectors.toList());
        logConsumer.accept(String.format("Received %d pull-requests, while %d are matching source/target filters", pullRequests.size(), filteredPullRequests.size()));

        printPullRequestTitles(logConsumer, pullRequests, "Received pull-requests:");
        printPullRequestTitles(logConsumer, filteredPullRequests, "Matching pull-requests:");

        if (!filteredPullRequests.isEmpty()) {
            //users
            Set<String> userUrls = filteredPullRequests.stream().map(PullRequest::getUser).map(PullRequestUser::getUrl).collect(Collectors.toSet());
            logConsumer.accept("Fetching PR owners information ...");
            int counter = 0;
            Map<String, User> login2User = new HashMap<>();
            for (String url : userUrls) {
                User user = getEntity(url, User.class);
                login2User.put(user.getLogin(), user);
                if (counter > 0 && counter % 10 == 0) {
                    logConsumer.accept("Fetching PR owners information " + counter * 100 / userUrls.size() + "%");
                }
                counter++;
            }
            Set<String> usersWithoutMails = login2User.values().stream().filter(u -> u.getEmail() == null).map(u -> u.getLogin()).collect(Collectors.toSet());
            if (!usersWithoutMails.isEmpty()) {
                logConsumer.accept("Note : Some users doesn't have defined public email in their profile. For such users, SCM user will contain their login name:  " + usersWithoutMails);
            }
            logConsumer.accept("Fetching PR owners information is done");

            logConsumer.accept("Fetching commits ...");
            counter = 0;
            for (PullRequest pr : filteredPullRequests) {
                //commits are returned in asc order by update time , therefore we need to get all before filtering , therefore page size equals to max total
                List<Commit> commits = getPagedEntities(pr.getCommitsUrl(), Commit.class, parameters.getMaxCommitsToFetch(), parameters.getMaxCommitsToFetch(), parameters.getMinUpdateTime());

                //commits
                List<com.hp.octane.integrations.dto.scm.SCMCommit> dtoCommits = new ArrayList<>();
                for (Commit commit : commits) {
                    com.hp.octane.integrations.dto.scm.SCMCommit dtoCommit = dtoFactory.newDTO(com.hp.octane.integrations.dto.scm.SCMCommit.class)
                            .setRevId(commit.getSha())
                            .setComment(commit.getCommit().getMessage())
                            .setUser(getUserName(commit.getCommit().getCommitter().getEmail(), commit.getCommit().getCommitter().getName()))
                            .setUserEmail(commit.getCommit().getCommitter().getEmail())
                            .setTime(FetchUtils.convertISO8601DateStringToLong(commit.getCommit().getCommitter().getDate()))
                            .setParentRevId(commit.getParents().get(0).getSha());
                    dtoCommits.add(dtoCommit);
                }

                SCMRepository sourceRepository = buildScmRepository(parameters.isUseSSHFormat(), pr.getHead());
                SCMRepository targetRepository = buildScmRepository(parameters.isUseSSHFormat(), pr.getBase());

                User prAuthor = login2User.get(pr.getUser().getLogin());
                String userId = getUserName(commitUserIdPicker, prAuthor.getEmail(), prAuthor.getLogin());
                com.hp.octane.integrations.dto.scm.PullRequest dtoPullRequest = dtoFactory.newDTO(com.hp.octane.integrations.dto.scm.PullRequest.class)
                        .setId(Integer.toString(pr.getNumber()))
                        .setTitle(pr.getTitle())
                        .setDescription(pr.getBody())
                        .setState(pr.getState())
                        .setCreatedTime(FetchUtils.convertISO8601DateStringToLong(pr.getCreatedAt()))
                        .setUpdatedTime(FetchUtils.convertISO8601DateStringToLong(pr.getUpdatedAt()))
                        .setMergedTime(FetchUtils.convertISO8601DateStringToLong(pr.getMergedAt()))
                        .setIsMerged(pr.getMergedAt() != null)
                        .setAuthorName(userId)
                        .setAuthorEmail(prAuthor.getEmail())
                        .setClosedTime(FetchUtils.convertISO8601DateStringToLong(pr.getClosedAt()))
                        .setSelfUrl(pr.getHtmlUrl())
                        .setSourceRepository(sourceRepository)
                        .setTargetRepository(targetRepository)
                        .setCommits(dtoCommits);
                result.add(dtoPullRequest);

                if (counter > 0 && counter % 25 == 0) {
                    logConsumer.accept("Fetching commits " + counter * 100 / filteredPullRequests.size() + "%");
                }
                counter++;
            }
            logConsumer.accept("Fetching commits is done");
            getRateLimitationInfo(apiUrl, logConsumer);
            logConsumer.accept("Pull requests are ready");
        } else {
            logConsumer.accept("No new/updated PR is found.");
        }
        return result;
    }

    private void printPullRequestTitles(Consumer<String> logConsumer, List<PullRequest> pullRequests, String textToPrint) {
        if (pullRequests.isEmpty()) {
            return;
        }

        logConsumer.accept(textToPrint);
        for (PullRequest pr : pullRequests) {
            String prTitle = (null == pr.getTitle()) ? "<no title>" : pr.getTitle();
            logConsumer.accept(prTitle);
        }
    }

    private SCMRepository buildScmRepository(boolean useSSHFormat, PullRequestRepo ref) {
        String url = "unknown repository";
        if (ref.getRepo() != null) {
            if (useSSHFormat && ref.getRepo().getSsh_url() != null) {
                url = ref.getRepo().getSsh_url();
            } else {
                url = ref.getRepo().getClone_url();
            }
        }
        return dtoFactory.newDTO(SCMRepository.class)
                .setUrl(url)
                .setBranch(ref.getRef())
                .setType(SCMType.GIT);
    }

    /**
     * RE
     *
     * @param baseUrl
     * @param logConsumer
     * @return
     * @throws IOException
     */
    private RateLimitationInfo getRateLimitationInfo(String baseUrl, Consumer<String> logConsumer) throws IOException {
        String rateUrl = baseUrl + "/rate_limit";
        OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class).setUrl(rateUrl).setMethod(HttpMethod.GET);
        OctaneResponse response = restClient.executeRequest(request);
        if (response.getStatus() == HttpStatus.SC_OK && response.getHeaders().containsKey("X-RateLimit-Limit")) {
            RateLimitationInfo info = new RateLimitationInfo();
            fillRateLimitationInfo(response, info);

            long minToNextReset = (info.getReset() - System.currentTimeMillis() / 1000) / 60;
            logConsumer.accept(String.format("RateLimit Info: Limit-%s; Remaining-%s; Reset in %s min. ", info.getLimit(), info.getRemaining(), minToNextReset));
            return info;
        } else {
            return null;
        }
    }

    private void fillRateLimitationInfo(OctaneResponse response, RateLimitationInfo info) {
        Map<String, String> toLowerCaseMapping = response.getHeaders().keySet().stream().collect(Collectors.toMap(String::toLowerCase, Function.identity()));
        int limit = (int) getRateLimitationInfoValue("X-RateLimit-Limit", toLowerCaseMapping, response);
        int remaining = (int) getRateLimitationInfoValue("X-RateLimit-Remaining", toLowerCaseMapping, response);
        int used = (int) getRateLimitationInfoValue("X-RateLimit-Used", toLowerCaseMapping, response);
        long reset = getRateLimitationInfoValue("X-RateLimit-Reset", toLowerCaseMapping, response);
        info.setLimit(limit).setRemaining(remaining).setUsed(used).setReset(reset);
    }

    private long getRateLimitationInfoValue(String key, Map<String, String> toLowerCaseMapping, OctaneResponse response) {
        String actualKey = toLowerCaseMapping.get(key.toLowerCase());
        if (actualKey != null) {
            return Long.parseLong(response.getHeaders().get(actualKey));
        } else {
            return 0L;
        }
    }

    /***
     *
     * @param url
     * @param entityType
     * @param pageSize
     * @param maxTotal
     * @param minUpdateTime
     * @param <T>
     * @return
     */
    private <T extends Entity & SupportUpdatedTime> List<T> getPagedEntities(String url, Class<T> entityType, int pageSize, int maxTotal, long minUpdateTime) {
        try {
            List<T> result = new ArrayList<>();
            boolean finished;
            String myUrl = url + (url.contains("?") ? "" : "?") + "&per_page=" + pageSize;

            do {
                finished = true;
                OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class).setUrl(myUrl).setMethod(HttpMethod.GET);
                OctaneResponse response = restClient.executeRequest(request);
                List<T> collection = JsonConverter.convertCollection(response.getBody(), entityType);
                result.addAll(collection);

                myUrl = getNextPageLink(response);
                if (myUrl != null) {
                    finished = false;
                }

                //remove outdated items
                for (int i = result.size() - 1; i >= 0 && minUpdateTime > 0; i--) {
                    if (result.get(i).getUpdatedTime() <= minUpdateTime) {
                        result.remove(i);
                    }
                }

                //remove exceeding items
                while (result.size() > maxTotal) {
                    result.remove(result.size() - 1);
                    finished = true;
                }
            } while (!finished);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to getPagedEntities : " + e.getMessage(), e);
        }
    }


    private <T extends Entity> T getEntity(String url, Class<T> entityType) {
        return getEntity(url, entityType, null);
    }

    private <T extends Entity> T getEntity(String url, Class<T> entityType, RateLimitationInfo rateLimitationInfo) {
        try {
            OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class).setUrl(url).setMethod(HttpMethod.GET);
            OctaneResponse response = restClient.executeRequest(request);
            if (response.getStatus() == HttpStatus.SC_NOT_FOUND) {
                throw new ResourceNotFoundException(String.format("URL %s not found", url));
            }

            if (rateLimitationInfo != null) {
                fillRateLimitationInfo(response, rateLimitationInfo);
            }
            return JsonConverter.convert(response.getBody(), entityType);
        } catch (ResourceNotFoundException notFoundException) {
            throw notFoundException;
        } catch (Exception e) {
            throw new RuntimeException("Failed to getEntity : " + e.getMessage(), e);
        }
    }

    private String getNextPageLink(OctaneResponse response) {
        String linkHeaderValue = response.getHeaders().get("Link");
        if (linkHeaderValue != null) {
            //<https://api.github.com/repositories/6774631/pulls?state=all&page=2>; rel="next", <https://api.github.com/repositories/6774631/pulls?state=all&page=10>; rel="last"
            String[] linksArr = linkHeaderValue.split(",");
            for (String link : linksArr) {
                if (link.endsWith("rel=\"next\"")) {
                    String[] segments = link.split(";");
                    if (segments.length == 2) {
                        String next = segments[0].trim();
                        if (next.startsWith("<") && next.endsWith(">")) {
                            return next.substring(1, next.length() - 1);
                        }
                    }
                }
            }
        }
        return null;
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

    @Override
    public SCMRepositoryLinks parseSCMRepositoryLinks(String responseBody) throws JsonProcessingException {
        Repo repo = com.hp.octane.integrations.services.pullrequestsandbranches.github.JsonConverter.convert(responseBody, Repo.class);
        SCMRepositoryLinks links = dtoFactory.newDTO(SCMRepositoryLinks.class).setHttpUrl(repo.getClone_url()).setSshUrl(repo.getSsh_url());
        return links;
    }
}
