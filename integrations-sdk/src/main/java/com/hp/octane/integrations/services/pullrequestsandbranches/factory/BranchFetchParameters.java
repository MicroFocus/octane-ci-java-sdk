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

import java.io.Serializable;

public class BranchFetchParameters implements Serializable {
    private String repoUrl;
    private String repoUrlSsh;
    private String filter;
    private Integer pageSize;
    private Integer activeBranchDays;
    private Integer maxBranchesToFill;
    private Integer searchBranchOctaneRootRepositoryId;
    private boolean useSSHFormat;

    public static final int DEFAULT_PAGE_SIZE = 1000;
    public static final int DEFAULT_MAX_BRANCHES_TO_FILL = 1000;
    public static final int DEFAULT_ACTIVE_BRANCH_DAYS = 60;

    public String getRepoUrl() {
        return repoUrl;
    }

    public BranchFetchParameters setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
        return this;
    }

    public String getFilter() {
        return filter;
    }

    public BranchFetchParameters setFilter(String filter) {
        this.filter = filter;
        return this;
    }

    public int getMaxBranchesToFill() {

        return maxBranchesToFill == null ? DEFAULT_MAX_BRANCHES_TO_FILL : maxBranchesToFill;
    }

    public BranchFetchParameters setMaxBranchesToFill(Integer maxBranchesToFill) {
        this.maxBranchesToFill = maxBranchesToFill;
        return this;
    }

    public int getActiveBranchDays() {
        return activeBranchDays == null ? DEFAULT_ACTIVE_BRANCH_DAYS : activeBranchDays;
    }

    public BranchFetchParameters setActiveBranchDays(Integer activeBranchDays) {
        this.activeBranchDays = activeBranchDays;
        return this;
    }

    public int getPageSize() {
        return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
    }

    public BranchFetchParameters setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("\n");
        sb.append("Filter       : ").append(getFilter()).append("\n");
        sb.append("Page size    : ").append(getPageSize()).append("\n");
        return sb.toString();
    }

    public String getRepoUrlSsh() {
        return repoUrlSsh;
    }

    public BranchFetchParameters setRepoUrlSsh(String repoUrlSsh) {
        this.repoUrlSsh = repoUrlSsh;
        return this;
    }

    public boolean isUseSSHFormat() {
        return useSSHFormat;
    }

    public BranchFetchParameters setUseSSHFormat(boolean useSSHFormat) {
        this.useSSHFormat = useSSHFormat;
        return this;
    }

    public int getSearchBranchOctaneRootRepositoryId() {
        return this.searchBranchOctaneRootRepositoryId == null ? 0 : this.searchBranchOctaneRootRepositoryId;
    }

    public BranchFetchParameters setSearchBranchOctaneRootRepositoryId(Integer searchBranchOctaneRootRepositoryId) {
        this.searchBranchOctaneRootRepositoryId = searchBranchOctaneRootRepositoryId;
        return this;
    }
}
