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
package com.hp.octane.integrations.services.pullrequestsandbranches.factory;

import java.io.Serializable;

public class PullRequestFetchParameters implements Serializable {
    private String repoUrl;
    private String repoUrlSsh;
    private String sourceBranchFilter;
    private String targetBranchFilter;
    private Integer pageSize;
    private Integer maxPRsToFetch;
    private Integer maxCommitsToFetch;
    private Integer searchBranchOctaneRootRepositoryId;
    private Long minUpdateTime;
    private boolean useSSHFormat;

    public static final int DEFAULT_PAGE_SIZE = 30;
    public static final int DEFAULT_MAX_PRS = 100;
    public static final int DEFAULT_MAX_COMMITS = 100;
    public static final long DEFAULT_MIN_UPDATE_DATE = 999;


    public String getRepoUrl() {
        return repoUrl;
    }

    public PullRequestFetchParameters setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
        return this;
    }

    public String getSourceBranchFilter() {
        return sourceBranchFilter;
    }

    public PullRequestFetchParameters setSourceBranchFilter(String sourceBranchFilter) {
        this.sourceBranchFilter = sourceBranchFilter;
        return this;
    }

    public String getTargetBranchFilter() {
        return targetBranchFilter;
    }

    public PullRequestFetchParameters setTargetBranchFilter(String targetBranchFilter) {
        this.targetBranchFilter = targetBranchFilter;
        return this;
    }

    public int getPageSize() {
        return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
    }

    public PullRequestFetchParameters setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public int getMaxPRsToFetch() {
        return maxPRsToFetch == null ? DEFAULT_MAX_PRS : maxPRsToFetch;
    }

    public PullRequestFetchParameters setMaxPRsToFetch(Integer maxPRsToFetch) {
        this.maxPRsToFetch = maxPRsToFetch;
        return this;
    }

    public int getMaxCommitsToFetch() {
        return maxCommitsToFetch == null ? DEFAULT_MAX_COMMITS : maxCommitsToFetch;
    }

    public PullRequestFetchParameters setMaxCommitsToFetch(Integer maxCommitsToFetch) {
        this.maxCommitsToFetch = maxCommitsToFetch;
        return this;
    }

    public long getMinUpdateTime() {return minUpdateTime == null ? DEFAULT_MIN_UPDATE_DATE : minUpdateTime;
    }

    public PullRequestFetchParameters setMinUpdateTime(Long minUpdateTime) {
        this.minUpdateTime = minUpdateTime;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("\n");
        sb.append("Min update date      : ").append(getMinUpdateTime()).append("\n");
        sb.append("Source branch filter : ").append(getSourceBranchFilter()).append("\n");
        sb.append("Target branch filter : ").append(getTargetBranchFilter()).append("\n");
        sb.append("Max PRs to fetch     : ").append(getMaxPRsToFetch()).append("\n");
        sb.append("Max commits to fetch : ").append(getMaxCommitsToFetch()).append("\n");
        sb.append("Page size            : ").append(getPageSize()).append("\n");
        sb.append("Use SSH Format       : ").append(isUseSSHFormat()).append("\n");

        return sb.toString();
    }

    public String getRepoUrlSsh() {
        return repoUrlSsh;
    }

    public PullRequestFetchParameters setRepoUrlSsh(String repoUrlSsh) {
        this.repoUrlSsh = repoUrlSsh;
        return this;
    }

    public boolean isUseSSHFormat() {
        return useSSHFormat;
    }

    public PullRequestFetchParameters setUseSSHFormat(boolean useSSHFormat) {
        this.useSSHFormat = useSSHFormat;
        return this;
    }

    public int getSearchBranchOctaneRootRepositoryId() {
        return this.searchBranchOctaneRootRepositoryId == null ? 0 : this.searchBranchOctaneRootRepositoryId;
    }

    public PullRequestFetchParameters setSearchBranchOctaneRootRepositoryId(Integer searchBranchOctaneRootRepositoryId) {
        this.searchBranchOctaneRootRepositoryId = searchBranchOctaneRootRepositoryId;
        return this;
    }
}
