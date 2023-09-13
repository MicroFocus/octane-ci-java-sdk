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

package com.hp.octane.integrations.dto.scm.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.scm.PullRequest;
import com.hp.octane.integrations.dto.scm.SCMCommit;
import com.hp.octane.integrations.dto.scm.SCMRepository;

import java.util.List;

/**
 * SCMCommit DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PullRequestImpl implements PullRequest {

    private String id;
    private String state;
    private String title;
    private String description;
    private Long createdTime;
    private Long updatedTime;
    private Long mergedTime;
    private Long closedTime;
    private String authorName;
    private String authorEmail;
    private SCMRepository sourceRepository;
    private SCMRepository targetRepository;
    private String selfUrl;
    private List<SCMCommit> commits;
    private boolean isMerged;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public PullRequest setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public PullRequest setState(String state) {
        this.state = state;
        return this;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public PullRequest setTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public PullRequest setDescription(String description) {
        if (description != null && description.length() >= 4000) {
            this.description = description.substring(0, 3995) + "...";
        } else {
            this.description = description;
        }
        return this;
    }

    @Override
    public Long getCreatedTime() {
        return createdTime;
    }

    @Override
    public PullRequest setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
        return this;
    }

    @Override
    public Long getUpdatedTime() {
        return updatedTime;
    }

    @Override
    public PullRequest setUpdatedTime(Long updatedTime) {
        this.updatedTime = updatedTime;
        return this;
    }

    @Override
    public Long getMergedTime() {
        return mergedTime;
    }

    @Override
    public PullRequest setMergedTime(Long mergedTime) {
        this.mergedTime = mergedTime;
        return this;
    }

    @Override
    public Long getClosedTime() {
        return closedTime;
    }

    @Override
    public PullRequest setClosedTime(Long closedTime) {
        this.closedTime = closedTime;
        return this;
    }

    @Override
    public String getAuthorName() {
        return authorName;
    }

    @Override
    public PullRequest setAuthorName(String authorName) {
        this.authorName = authorName;
        return this;
    }

    @Override
    public String getAuthorEmail() {
        return authorEmail;
    }

    @Override
    public PullRequest setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
        return this;
    }

    @Override
    public SCMRepository getSourceRepository() {
        return sourceRepository;
    }

    @Override
    public PullRequest setSourceRepository(SCMRepository sourceScmRepository) {
        this.sourceRepository = sourceScmRepository;
        return this;
    }

    @Override
    public SCMRepository getTargetRepository() {
        return targetRepository;
    }

    @Override
    public PullRequest setTargetRepository(SCMRepository targetScmRepository) {
        this.targetRepository = targetScmRepository;
        return this;
    }

    @Override
    public String getSelfUrl() {
        return selfUrl;
    }

    @Override
    public PullRequest setSelfUrl(String selfUrl) {
        this.selfUrl = selfUrl;
        return this;
    }

    @Override
    public List<SCMCommit> getCommits() {
        return commits;
    }

    @Override
    public PullRequest setCommits(List<SCMCommit> commits) {
        this.commits = commits;
        return this;
    }

    @Override
    public boolean isMerged() {
        return this.isMerged;
    }

    @Override
    public PullRequest setIsMerged(boolean isMerged) {
        this.isMerged = isMerged;
        return this;
    }
}
