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
package com.hp.octane.integrations.services.pullrequestsandbranches.bitbucketserver.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Commit extends Entity implements SupportUpdatedTime {
    private String displayId;
    private String message;
    private long committerTimestamp;
    private long authorTimestamp;
    private UserDetails committer;
    private UserDetails author;
    private List<CommitParent> parents;

    public String getDisplayId() {
        return displayId;
    }

    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getCommitterTimestamp() {
        return committerTimestamp;
    }

    @Override
    public long getUpdatedTime() {
        return committerTimestamp;
    }

    public void setCommitterTimestamp(long committerTimestamp) {
        this.committerTimestamp = committerTimestamp;
    }

    public long getAuthorTimestamp() {
        return authorTimestamp;
    }

    public void setAuthorTimestamp(long authorTimestamp) {
        this.authorTimestamp = authorTimestamp;
    }

    public UserDetails getCommitter() {
        return committer;
    }

    public void setCommitter(UserDetails committer) {
        this.committer = committer;
    }

    public UserDetails getAuthor() {
        return author;
    }

    public void setAuthor(UserDetails author) {
        this.author = author;
    }

    public List<CommitParent> getParents() {
        return parents;
    }

    public void setParents(List<CommitParent> parents) {
        this.parents = parents;
    }
}
