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

package com.hp.octane.integrations.services.pullrequestsandbranches;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.entities.EntityConstants;
import com.hp.octane.integrations.dto.scm.Branch;
import com.hp.octane.integrations.dto.scm.PullRequest;
import com.hp.octane.integrations.dto.scm.SCMRepositoryLinks;
import com.hp.octane.integrations.dto.scm.SCMType;
import com.hp.octane.integrations.exceptions.OctaneBulkException;
import com.hp.octane.integrations.exceptions.ResourceNotFoundException;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.entities.QueryHelper;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.*;
import com.hp.octane.integrations.services.pullrequestsandbranches.github.GithubV3FetchHandler;
import com.hp.octane.integrations.services.pullrequestsandbranches.gitlab.GitlabServerFetchHandler;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * Default implementation of tests service
 */

final class PullRequestAndBranchServiceImpl implements PullRequestAndBranchService {
    private static final Logger logger = LogManager.getLogger(PullRequestAndBranchServiceImpl.class);
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();

    private final OctaneSDK.SDKServicesConfigurer configurer;
    private final RestService restService;
    private final EntitiesService entitiesService;
    private final File persistenceFile;
    private Map<String, PRItem> prItems;
    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final String REMOVE_PREFIX = "origin/";


    PullRequestAndBranchServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService, EntitiesService entitiesService) {
        if (configurer == null) {
            throw new IllegalArgumentException("invalid configurer");
        }
        if (restService == null) {
            throw new IllegalArgumentException("rest service MUST NOT be null");
        }
        if (entitiesService == null) {
            throw new IllegalArgumentException("entities service MUST NOT be null");
        }
        this.configurer = configurer;
        this.restService = restService;
        this.entitiesService = entitiesService;

        logger.info(configurer.octaneConfiguration.getLocationForLog() + "initialized SUCCESSFULLY");

        if (configurer.pluginServices.getAllowedOctaneStorage() != null) {
            File storageDirectory = new File(configurer.pluginServices.getAllowedOctaneStorage(), "nga" + File.separator + configurer.octaneConfiguration.getInstanceId());
            if (!storageDirectory.mkdirs()) {
                logger.debug(configurer.octaneConfiguration.getLocationForLog() + "instance folder considered as exist");
            }
            persistenceFile = new File(storageDirectory, "pr-fetchers.json");
            logger.info(configurer.octaneConfiguration.getLocationForLog() + "hosting plugin PROVIDE available storage, PR persistence enabled");

            if (persistenceFile.exists()) {
                try {
                    JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, PRItem.class);
                    List<PRItem> list = objectMapper.readValue(persistenceFile, type);
                    prItems = list.stream().collect(Collectors.toMap(PRItem::getKey, Function.identity()));
                } catch (IOException e) {
                    logger.info(configurer.octaneConfiguration.getLocationForLog() + "failed to read PR persisted file");
                }
            } else {
                prItems = new HashMap<>();
            }
        } else {
            persistenceFile = null;
            prItems = new HashMap<>();
            logger.info(configurer.octaneConfiguration.getLocationForLog() + "hosting plugin DO NOT PROVIDE available storage, PR persistence disabled");
        }
    }

    @Override
    public void sendPullRequests(List<PullRequest> pullRequests, String workspaceId, PullRequestFetchParameters pullRequestFetchParameters, Consumer<String> logConsumer) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(RestService.CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());

        String url = configurer.octaneConfiguration.getUrl() +
                RestService.SHARED_SPACE_API_PATH_PART + configurer.octaneConfiguration.getSharedSpace() +
                "/workspaces/" + workspaceId + RestService.ANALYTICS_CI_PATH_PART + "pull-requests/";

        int sentCounter = 0;
        List<List<PullRequest>> subSets = ListUtils.partition(pullRequests, 200);
        for (List<PullRequest> list : subSets) {
            String json = dtoFactory.dtoCollectionToJson(list);
            OctaneRequest octaneRequest = dtoFactory.newDTO(OctaneRequest.class)
                    .setMethod(HttpMethod.PUT)
                    .setUrl(url)
                    .setHeaders(headers)
                    .setBody(json);

            OctaneResponse octaneResponse = restService.obtainOctaneRestClient().execute(octaneRequest);
            if (octaneResponse.getStatus() != HttpStatus.SC_OK) {
                if (octaneResponse.getStatus() == HttpStatus.SC_NOT_FOUND) {
                    throw new ResourceNotFoundException("Failed to sendPullRequests : received 404 status. Validate that you use correct workspace id and ALM Octane version is greater than " + PullRequestAndBranchService.BRANCH_COLLECTION_SUPPORTED_VERSION);
                } else {
                    throw new RuntimeException("Failed to sendPullRequests : (" + octaneResponse.getStatus() + ")" + octaneResponse.getBody());
                }
            } else {
                sentCounter += list.size();
                logConsumer.accept(String.format("Sent %s/%s pull requests.", sentCounter, pullRequests.size()));
            }
        }

        long lastUpdateTime = pullRequests.stream().map(PullRequest::getUpdatedTime).max(Comparator.naturalOrder()).orElse(0L);
        savePullRequestLastUpdateTime(workspaceId, pullRequestFetchParameters.getRepoUrl(), lastUpdateTime);
        logConsumer.accept("Last update time set to " + lastUpdateTime);
    }

    @Override
    public long getPullRequestLastUpdateTime(String workspaceId, String repoUrl) {
        String key = PRItem.buildKey(workspaceId, repoUrl);
        PRItem item = prItems.get(key);
        return item == null ? 0 : item.getLastUpdated();
    }

    @Override
    public BranchSyncResult syncBranchesToOctane(FetchHandler fetcherHandler, BranchFetchParameters fp, Long workspaceId, CommitUserIdPicker idPicker, Consumer<String> logConsumer) throws IOException {

        //update ssh url
        String baseUrl = fetcherHandler.getRepoApiPath(fp.getRepoUrl());
        if (!(fetcherHandler instanceof GitlabServerFetchHandler)) {
            logConsumer.accept(fetcherHandler.getClass().getSimpleName() + " handler, Base url : " + baseUrl);
        }

        SCMRepositoryLinks links = fetcherHandler.pingRepository(baseUrl, logConsumer);
        fp.setRepoUrlSsh(links.getSshUrl());
        if (fp.isUseSSHFormat()) {
            logConsumer.accept("Repo ssh format url : " + fp.getRepoUrlSsh());
        }
        String repoUrlForOctane = fp.isUseSSHFormat() ? fp.getRepoUrlSsh() : fp.getRepoUrl();


        //LOAD FROM CACHE
        boolean supportCaching = fetcherHandler instanceof GithubV3FetchHandler;
        Map<String, Long> sha2DateMapCache = null;
        if (supportCaching) {
            sha2DateMapCache = loadBranchCommitsFromCache(repoUrlForOctane, logConsumer);
        }

        //FETCH FROM CI SERVER
        List<Branch> ciServerBranches = fetcherHandler.fetchBranches(fp, sha2DateMapCache, logConsumer);

        List<Entity> rootRepositoryForSearchList = getRepositoryRootsById(fp.getSearchBranchOctaneRootRepositoryId(), workspaceId);
        if(!rootRepositoryForSearchList.isEmpty()){

            Entity rootRepoForSearch = rootRepositoryForSearchList.get(0);
            logConsumer.accept(String.format(
                    "Filtering out the branches that already exist in the root repository with the configured id: %s",
                    rootRepoForSearch.getId()));
            List<Entity> octaneRepositoryBranches = getRepositoryBranches(rootRepoForSearch.getId(), workspaceId, false);

            ciServerBranches = ciServerBranches
                    .stream()
                    .filter(branch -> !(octaneRepositoryBranches
                                                .stream().map(b->b.getField(EntityConstants.ScmRepository.NAME_FIELD).toString())
                                                .collect(Collectors.toSet())
                                                .contains(branch.getName())
                                        && octaneRepositoryBranches
                                                .stream().map(b->b.getField(EntityConstants.ScmRepository.LAST_COMMIT_SHA_FIELD).toString())
                                                .collect(Collectors.toSet())
                                                .contains(branch.getLastCommitSHA())))
                    .collect(Collectors.toList());
        }
        else{
            logConsumer.accept("The root repository id is not configured or no root repository exists with the configured id in ALM Octane.");
        }

        Map<String, Branch> ciServerBranchMap = ciServerBranches.stream().collect(Collectors.toMap(Branch::getName, Function.identity()));

        //SAVE TO  CACHE
        if (supportCaching) {
            saveBranchCommitsToCache(repoUrlForOctane, logConsumer, sha2DateMapCache, ciServerBranches);
        }

        //GET BRANCHES FROM OCTANE
        String repoShortName = FetchUtils.getRepoShortName(fp.getRepoUrl());

        List<Entity> roots = new ArrayList<>(getRepositoryRoots(repoUrlForOctane, workspaceId));

        List<Entity> octaneBranches = new ArrayList<>();

        String rootId = "";
        if (!roots.isEmpty()) {
            rootId = roots.get(0).getId();
            octaneBranches = getRepositoryBranches(rootId, workspaceId, false);
            logConsumer.accept("Found repository root with id " + rootId);
        }

        if (octaneBranches == null) {
            octaneBranches = Collections.emptyList();
        }

        List<Pattern> filterPatterns = FetchUtils.buildPatterns(fp.getFilter());
        Map<String, List<Entity>> octaneBranchMap = octaneBranches.stream()
                .filter(br -> FetchUtils.isBranchMatch(filterPatterns, br.getName()))
                .collect(groupingBy(b -> b.getName()));
        logConsumer.accept("Found " + octaneBranches.size() + " branches in ALM Octane related to defined filter.");

        //GENERATE UPDATES

        BranchSyncResult result = new BranchSyncResult();

        //DELETED
        octaneBranchMap.entrySet().stream().filter(entry -> !ciServerBranchMap.containsKey(entry.getKey()))
                .map(e -> e.getValue()).flatMap(Collection::stream)
                .map(e -> dtoFactory.newDTO(Branch.class).setOctaneId(e.getId()).setName(e.getName()))
                .forEach(b -> result.getDeleted().add(b));

        //NEW AND UPDATES
        ciServerBranches.forEach(ciBranch -> {
            if (ciBranch.isPartial()) {
                //SKIP if branch is partial (it can happen because of rate limitations or because branch is merged to master or not active for long time)
                return;
            }
            List<Entity> octaneBranchList = octaneBranchMap.get(ciBranch.getName());
            if (octaneBranchList == null) {//not exist in octane, check if to add
                long diff = System.currentTimeMillis() - ciBranch.getLastCommitTime();
                long diffDays = TimeUnit.MILLISECONDS.toDays(diff);
                if (diffDays < fp.getActiveBranchDays()) {
                    result.getCreated().add(ciBranch);
                }
            } else {//check for update
                octaneBranchList.forEach(octaneBranch -> {
                    if (!ciBranch.getLastCommitSHA().equals(octaneBranch.getField(EntityConstants.ScmRepository.LAST_COMMIT_SHA_FIELD)) ||
                            !ciBranch.getIsMerged().equals(octaneBranch.getField(EntityConstants.ScmRepository.IS_MERGED_FIELD))) {
                        ciBranch.setOctaneId(octaneBranch.getId());
                        result.getUpdated().add(ciBranch);
                    }
                });
            }
        });

        //SEND TO OCTANE
        if (!result.getDeleted().isEmpty()) {
            List<Entity> toDelete = result.getDeleted().stream().map(b -> buildOctaneBranchForUpdateAsDeleted(b)).collect(Collectors.toList());
            entitiesService.updateEntities(workspaceId, EntityConstants.ScmRepository.COLLECTION_NAME, toDelete);
            logConsumer.accept("Deleted branches : " + toDelete.size());
        }
        if (!result.getUpdated().isEmpty()) {
            List<Entity> toUpdate = result.getUpdated().stream().map(b -> buildOctaneBranchForUpdate(b, idPicker)).collect(Collectors.toList());
            entitiesService.updateEntities(workspaceId, EntityConstants.ScmRepository.COLLECTION_NAME, toUpdate);
            logConsumer.accept("Updated branches : " + toUpdate.size());
        }
        if (!result.getCreated().isEmpty()) {
            Entity createdRoot = createRepositoryRoot(repoUrlForOctane, repoShortName, workspaceId);
            String createdRootId = createdRoot.getId();
            logConsumer.accept("Repository root is created with id " + rootId);

            List<Entity> toCreate = result.getCreated().stream().map(b -> buildOctaneBranchForCreate(createdRootId, b, idPicker)).collect(Collectors.toList());
            try {
                entitiesService.postEntities(workspaceId, EntityConstants.ScmRepository.COLLECTION_NAME, toCreate);
                logConsumer.accept("New branches : " + toCreate.size());
            } catch (OctaneBulkException bulkException) {
                logConsumer.accept(String.format("New branches created: %s, failed to create %s branches",
                        (toCreate.size() - bulkException.getData().getErrors().size()), bulkException.getData().getErrors().size()));

                //handling previously deleted branches. (new branches were created with the name that already exist in Octane but set as deleted)
                boolean hasDuplicatedException = bulkException.getData().getErrors().stream()
                        .filter(ex -> EntityConstants.Errors.DUPLICATE_ERROR_CODE.equals(ex.getErrorCode())).findAny().isPresent();
                Map<String, Entity> deletedBranchesInOctane = !hasDuplicatedException ? Collections.emptyMap() :
                        getRepositoryBranches(rootId, workspaceId, true).stream()
                                .collect(Collectors.toMap(e -> e.getStringValue(EntityConstants.ScmRepository.NAME_FIELD), Function.identity()));

                //try to update duplicates
                List<Entity> deletedBranchesToUpdate = new ArrayList<>();
                bulkException.getData().getErrors().forEach(ex -> {
                    //if post was done with only one entity - index will be null
                    int index = ex.getIndex() == null ? 0 : ex.getIndex();
                    Branch branch = result.getCreated().get(index);
                    if (EntityConstants.Errors.DUPLICATE_ERROR_CODE.equals(ex.getErrorCode())) {
                        Entity octaneEntity = deletedBranchesInOctane.get(branch.getName());
                        if (octaneEntity != null) {
                            branch.setOctaneId(octaneEntity.getId());
                            deletedBranchesToUpdate.add(buildOctaneBranchForUpdate(branch, idPicker).setField(EntityConstants.ScmRepository.IS_DELETED_FIELD, false));
                        } else {
                            logConsumer.accept("Failed to create/update branch : " + branch.getName());
                        }
                    } else {
                        logConsumer.accept(String.format("Failed to create branch %s : %s ", branch.getName(), ex.getDescriptionTranslated()));
                    }
                });

                if (!deletedBranchesToUpdate.isEmpty()) {
                    entitiesService.updateEntities(workspaceId, EntityConstants.ScmRepository.COLLECTION_NAME, deletedBranchesToUpdate);
                    logConsumer.accept("New branches that appear as deleted in ALM OCtane : " + deletedBranchesToUpdate.size());
                }
            }
        }
        if (result.getDeleted().isEmpty() && result.getUpdated().isEmpty() && result.getCreated().isEmpty()) {
            logConsumer.accept("No changes are found.");
        }

        return result;
    }

    private void saveBranchCommitsToCache(String repoUrlForOctane, Consumer<String> logConsumer, Map<String, Long> sha2DateMapCache, List<Branch> ciServerBranches) {
        HashMap<String, Long> newSha2DateMapCache = new HashMap<>();
        newSha2DateMapCache.putAll(sha2DateMapCache);
        ciServerBranches.stream().filter(b -> b.getLastCommitTime() != null).forEach(b -> newSha2DateMapCache.put(b.getLastCommitSHA(), b.getLastCommitTime()));
        File cacheFile = getFileForBranchCaching(repoUrlForOctane);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(cacheFile, newSha2DateMapCache);
            logConsumer.accept(String.format("Cache of commits is saved with %s items ", newSha2DateMapCache.size()));
        } catch (Exception e) {
            logConsumer.accept("Failed to  save commit cache : " + e.getMessage());
        }
    }

    private Map<String, Long> loadBranchCommitsFromCache(String repoUrl, Consumer<String> logConsumer) {
        Map<String, Long> sha2DateMapCache = Collections.emptyMap();

        TypeReference<HashMap<String, Long>> typeRef = new TypeReference<HashMap<String, Long>>() {
        };
        File cacheFile = getFileForBranchCaching(repoUrl);
        if (cacheFile.exists()) {
            try {
                sha2DateMapCache = objectMapper.readValue(cacheFile, typeRef);
                logConsumer.accept(String.format("Cache of commits is loaded with %s items ", sha2DateMapCache.size()));
            } catch (Exception e) {
                logConsumer.accept("Failed to load cache of commits : " + e.getMessage());
            }
        }

        return sha2DateMapCache;
    }

    private File getFileForBranchCaching(String url) {
        String urlReplacement = url.replace(":", "_").replace("/", "_").replaceAll("[<>:\"/\\|?*]", "_");

        String path = configurer.pluginServices.getAllowedOctaneStorage() + File.separator +
                "nga" + File.separator + configurer.octaneConfiguration.getInstanceId() + File.separator + "branchCache_" + urlReplacement;
        return new File(path);
    }

    private Entity createRepositoryRoot(String repoUrlForOctane, String repoShortName, Long workspaceId) {
        Entity entity = DTOFactory.getInstance().newDTO(Entity.class);
        entity.setType(EntityConstants.ScmRepositoryRoot.ENTITY_NAME);
        entity.setField(EntityConstants.ScmRepositoryRoot.URL_FIELD, repoUrlForOctane);
        entity.setField(EntityConstants.ScmRepositoryRoot.NAME_FIELD, repoShortName);
        entity.setField(EntityConstants.ScmRepositoryRoot.SCM_TYPE_FIELD, SCMType.GIT.getOctaneId());
        List<Entity> results = entitiesService.postEntities(workspaceId, EntityConstants.ScmRepositoryRoot.COLLECTION_NAME, Arrays.asList(entity));

        return results.get(0);
    }

    @Override
    public boolean updateRepoTemplates(String repoUrl, Long workspaceId, RepoTemplates repoTemplates) {
        List<Entity> roots = getRepositoryRoots(repoUrl, workspaceId);
        int trialCounter = 0;
        while (roots.isEmpty() && trialCounter++ < 6) {
            logger.info(String.format("Wait to updateRepoTemplates - repo %s - %s", repoUrl, trialCounter));
            //pull request repo generate async way by CTP task, so might be delay in creation
            CIPluginSDKUtils.doWait(5000);
            roots = getRepositoryRoots(repoUrl, workspaceId);
        }
        if (roots.isEmpty()) {
            logger.info(String.format("UpdateRepoTemplates  - repo %s is not found in ALM Octane", repoUrl));
            return false;
        }

        Entity repo = roots.get(0);
        Entity entity = DTOFactory.getInstance().newDTO(Entity.class);
        entity.setField(EntityConstants.ScmRepositoryRoot.ID_FIELD, repo.getId());

        boolean needUpdate = false;
        if (SdkStringUtils.isNotEmpty(repoTemplates.getBranchFileTemplate()) &&
                SdkStringUtils.isEmpty(repo.getStringValue(EntityConstants.ScmRepositoryRoot.BRANCH_TEMPLATE))) {
            entity.setField(EntityConstants.ScmRepositoryRoot.BRANCH_TEMPLATE, repoTemplates.getBranchFileTemplate());
            needUpdate = true;
        }
        if (SdkStringUtils.isNotEmpty(repoTemplates.getDiffTemplate()) &&
                SdkStringUtils.isEmpty(repo.getStringValue(EntityConstants.ScmRepositoryRoot.DIFF_TEMPLATE))) {
            entity.setField(EntityConstants.ScmRepositoryRoot.DIFF_TEMPLATE, repoTemplates.getDiffTemplate());
            needUpdate = true;
        }
        if (SdkStringUtils.isNotEmpty(repoTemplates.getSourceViewTemplate()) &&
                SdkStringUtils.isEmpty(repo.getStringValue(EntityConstants.ScmRepositoryRoot.SOURCE_VIEW_TEMPLATE))) {
            entity.setField(EntityConstants.ScmRepositoryRoot.SOURCE_VIEW_TEMPLATE, repoTemplates.getSourceViewTemplate());
            needUpdate = true;
        }

        if (needUpdate) {
            entitiesService.updateEntities(workspaceId, EntityConstants.ScmRepositoryRoot.COLLECTION_NAME, Arrays.asList(entity));
        }
        return needUpdate;
    }

    private Entity buildOctaneBranchForUpdateAsDeleted(Branch branch) {
        Entity entity = DTOFactory.getInstance().newDTO(Entity.class);
        entity.setType(EntityConstants.ScmRepository.ENTITY_NAME);
        entity.setId(branch.getOctaneId());
        entity.setField(EntityConstants.ScmRepository.IS_DELETED_FIELD, true);
        return entity;
    }

    private Entity buildOctaneBranchForUpdate(Branch ciBranch, CommitUserIdPicker idPicker) {
        Entity entity = DTOFactory.getInstance().newDTO(Entity.class);
        entity.setType(EntityConstants.ScmRepository.ENTITY_NAME);
        if (ciBranch.getOctaneId() != null) {
            entity.setId(ciBranch.getOctaneId());
        }

        entity.setField(EntityConstants.ScmRepository.IS_MERGED_FIELD, ciBranch.getIsMerged());
        entity.setField(EntityConstants.ScmRepository.LAST_COMMIT_SHA_FIELD, ciBranch.getLastCommitSHA());
        entity.setField(EntityConstants.ScmRepository.LAST_COMMIT_TIME_FIELD, FetchUtils.convertLongToISO8601DateString(ciBranch.getLastCommitTime()));
        entity.setField(EntityConstants.ScmRepository.SCM_USER_FIELD, idPicker.getUserIdForCommit(ciBranch.getLastCommiterEmail(), ciBranch.getLastCommiterName()));
        entity.setField(EntityConstants.ScmRepository.SCM_USER_EMAIL_FIELD, ciBranch.getLastCommiterEmail());
        return entity;
    }

    private Entity buildOctaneBranchForCreate(String rootId, Branch ciBranch, CommitUserIdPicker idPicker) {
        Entity parent = DTOFactory.getInstance().newDTO(Entity.class);
        parent.setType(EntityConstants.ScmRepositoryRoot.ENTITY_NAME);
        parent.setId(rootId);

        Entity entity = buildOctaneBranchForUpdate(ciBranch, idPicker);
        entity.setField(EntityConstants.ScmRepository.PARENT_FIELD, parent);
        entity.setField(EntityConstants.ScmRepository.NAME_FIELD, ciBranch.getName());
        return entity;
    }

    private List<Entity> getRepositoryRoots(String repoUrl, Long workspaceId) {
        String rootByUrlCondition = QueryHelper.condition(EntityConstants.ScmRepositoryRoot.URL_FIELD, repoUrl);
        List<Entity> foundRoots = entitiesService.getEntities(workspaceId,
                EntityConstants.ScmRepositoryRoot.COLLECTION_NAME,
                Collections.singleton(rootByUrlCondition),
                Arrays.asList(EntityConstants.Base.ID_FIELD,
                        EntityConstants.ScmRepositoryRoot.BRANCH_TEMPLATE,
                        EntityConstants.ScmRepositoryRoot.DIFF_TEMPLATE,
                        EntityConstants.ScmRepositoryRoot.SOURCE_VIEW_TEMPLATE));
        return foundRoots;
    }
    private List<Entity> getRepositoryRootsById(Integer id, Long workspaceId) {
        String rootByUrlCondition = QueryHelper.condition(EntityConstants.ScmRepositoryRoot.ID_FIELD, id);
        List<Entity> foundRoots = entitiesService.getEntities(workspaceId,
                EntityConstants.ScmRepositoryRoot.COLLECTION_NAME,
                Collections.singleton(rootByUrlCondition),
                Arrays.asList(EntityConstants.Base.ID_FIELD,
                        EntityConstants.ScmRepositoryRoot.BRANCH_TEMPLATE,
                        EntityConstants.ScmRepositoryRoot.DIFF_TEMPLATE,
                        EntityConstants.ScmRepositoryRoot.SOURCE_VIEW_TEMPLATE,
                        EntityConstants.ScmRepositoryRoot.NAME_FIELD));
        return foundRoots;
    }

    private List<Entity> getRepositoryBranches(String repositoryRootId, Long workspaceId, boolean deleted) {
        String byParentIdCondition = QueryHelper.conditionRef(EntityConstants.ScmRepository.PARENT_FIELD, Long.parseLong(repositoryRootId));
        String notDeletedCondition = QueryHelper.condition(EntityConstants.ScmRepository.IS_DELETED_FIELD, deleted);
        List<Entity> foundBranches = entitiesService.getEntities(workspaceId,
                EntityConstants.ScmRepository.COLLECTION_NAME,
                Arrays.asList(byParentIdCondition, notDeletedCondition),
                Arrays.asList(EntityConstants.ScmRepository.NAME_FIELD,
                        EntityConstants.ScmRepository.IS_MERGED_FIELD,
                        EntityConstants.ScmRepository.LAST_COMMIT_SHA_FIELD,
                        EntityConstants.ScmRepository.LAST_COMMIT_TIME_FIELD));

        return foundBranches;
    }

    private synchronized void savePullRequestLastUpdateTime(String workspaceId, String repoUrl, long lastUpdateTime) {
        PRItem item = PRItem.create(workspaceId, repoUrl, lastUpdateTime);
        prItems.put(item.getKey(), item);
        if (persistenceFile != null) {
            try {
                objectMapper.writeValue(persistenceFile, prItems.values());
            } catch (IOException e) {
                logger.info(configurer.octaneConfiguration.getLocationForLog() + "failed to save PR persisted file");
            }
        }
    }

    public static class PRItem implements Serializable {
        private String workspace;
        private String repositoryUrl;
        private long lastUpdated;

        public static PRItem create(String workspace, String repositoryUrl, long lastUpdated) {
            PRItem item = new PRItem();
            item.workspace = workspace;
            item.repositoryUrl = repositoryUrl;
            item.lastUpdated = lastUpdated;
            return item;
        }

        @JsonIgnore
        public String getKey() {
            return buildKey(getWorkspace(), getRepositoryUrl());
        }

        public static String buildKey(String workspace, String repositoryUrl) {
            return workspace + "_" + repositoryUrl;
        }

        public String getWorkspace() {
            return workspace;
        }

        public String getRepositoryUrl() {
            return repositoryUrl;
        }

        public long getLastUpdated() {
            return lastUpdated;
        }
    }
}
