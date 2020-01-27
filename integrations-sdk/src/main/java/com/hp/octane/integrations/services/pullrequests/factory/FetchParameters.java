/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.hp.octane.integrations.services.pullrequests.factory;

import java.util.function.Consumer;

public class FetchParameters {
    private String repoUrl;
    private String sourceBranchFilter;
    private String targetBranchFilter;
    private Consumer<String> logConsumer;
    private Integer pageSize;
    private Integer maxPRsToFetch;
    private Integer maxCommitsToFetch;
    private long prStartUpdateDate;

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int DEFAULT_MAX_PRS = 100;
    public static final int DEFAULT_MAX_COMMITS = 100;


    public String getRepoUrl() {
        return repoUrl;
    }

    public FetchParameters setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
        return this;
    }

    public String getSourceBranchFilter() {
        return sourceBranchFilter;
    }

    public FetchParameters setSourceBranchFilter(String sourceBranchFilter) {
        this.sourceBranchFilter = sourceBranchFilter;
        return this;
    }

    public String getTargetBranchFilter() {
        return targetBranchFilter;
    }

    public FetchParameters setTargetBranchFilter(String targetBranchFilter) {
        this.targetBranchFilter = targetBranchFilter;
        return this;
    }

    public Consumer<String> getLogConsumer() {
        return logConsumer;
    }

    public FetchParameters setLogConsumer(Consumer<String> logConsumer) {
        this.logConsumer = logConsumer;
        return this;
    }

    public int getPageSize() {
        return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
    }

    public FetchParameters setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public int getMaxPRsToFetch() {
        return maxPRsToFetch == null ? DEFAULT_MAX_PRS : maxPRsToFetch;
    }

    public FetchParameters setMaxPRsToFetch(Integer maxPRsToFetch) {
        this.maxPRsToFetch = maxPRsToFetch;
        return this;
    }

    public int getMaxCommitsToFetch() {
        return maxCommitsToFetch == null ? DEFAULT_MAX_COMMITS : maxCommitsToFetch;
    }

    public FetchParameters setMaxCommitsToFetch(Integer maxCommitsToFetch) {
        this.maxCommitsToFetch = maxCommitsToFetch;
        return this;
    }

    public long getPrStartUpdateDate() {
        return prStartUpdateDate;
    }

    public FetchParameters setPrStartUpdateDate(long prStartUpdateDate) {
        this.prStartUpdateDate = prStartUpdateDate;
        return this;
    }

    public void printToLogConsumer(){
        logConsumer.accept("Min PR update date   : " + getPrStartUpdateDate());
        logConsumer.accept("Source branch filter : " + getSourceBranchFilter());
        logConsumer.accept("Target branch filter : " + getTargetBranchFilter());
        logConsumer.accept("Max PRs to fetch     : " + getMaxPRsToFetch());
        logConsumer.accept("Max commits to fetch : " + getMaxCommitsToFetch());
        logConsumer.accept("Page size            : " + getPageSize());
    }
}
