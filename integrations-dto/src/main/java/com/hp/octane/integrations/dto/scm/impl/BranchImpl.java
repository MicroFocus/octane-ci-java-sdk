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
package com.hp.octane.integrations.dto.scm.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.scm.Branch;

/**
 * SCMCommit DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BranchImpl implements Branch {

    private String octaneId;
    private String name;
    private String internalId;
    private Boolean isMerged;
    private String lastCommitSHA;
    private String lastCommitUrl;
    private Long lastCommitTime;
    private String lastCommiterName;
    private String lastCommiterEmail;
    private boolean isPartial = false;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Branch setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public Boolean getIsMerged() {
        return isMerged;
    }

    @Override
    public Branch setIsMerged(boolean isMerged) {
        this.isMerged = isMerged;
        return this;
    }

    @Override
    public String getLastCommitSHA() {
        return lastCommitSHA;
    }

    @Override
    public Branch setLastCommitSHA(String lastCommitSHA) {
        this.lastCommitSHA = lastCommitSHA;
        return this;
    }

    @Override
    public String getLastCommitUrl() {
        return lastCommitUrl;
    }

    @Override
    public Branch setLastCommitUrl(String lastCommitUrl) {
        this.lastCommitUrl = lastCommitUrl;
        return this;
    }

    @Override
    public Long getLastCommitTime() {
        return lastCommitTime;
    }

    @Override
    public Branch setLastCommitTime(Long lastCommitTime) {
        this.lastCommitTime = lastCommitTime;
        return this;
    }

    @Override
    public String getLastCommiterName() {
        return lastCommiterName;
    }

    @Override
    public Branch setLastCommiterName(String lastCommiterName) {
        this.lastCommiterName = lastCommiterName;
        return this;
    }

    @Override
    public String getLastCommiterEmail() {
        return lastCommiterEmail;
    }

    @Override
    public Branch setLastCommiterEmail(String lastCommiterEmail) {
        this.lastCommiterEmail = lastCommiterEmail;
        return this;
    }

    @Override
    public boolean isPartial() {
        return isPartial;
    }

    @Override
    public Branch setPartial(boolean partial) {
        isPartial = partial;
        return this;
    }

    @Override
    public String getOctaneId() {
        return octaneId;
    }

    @Override
    public Branch setOctaneId(String octaneId) {
        this.octaneId = octaneId;
        return this;
    }

    @Override
    public String getInternalId() {
        return internalId;
    }

    @Override
    public Branch setInternalId(String internalId) {
        this.internalId = internalId;
        return this;
    }
}
