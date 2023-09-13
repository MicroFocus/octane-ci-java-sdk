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
import com.hp.octane.integrations.dto.scm.SCMChange;
import com.hp.octane.integrations.dto.scm.SCMCommit;

import java.util.List;

/**
 * SCMCommit DTO
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class SCMCommitImpl implements SCMCommit {
	private Long time;
	private String user;
	private String userEmail;
	private String revId;
	private String parentRevId;
	private String comment;
	private List<SCMChange> changes;

	public Long getTime() {
		return time;
	}

	public SCMCommit setTime(Long time) {
		this.time = time;
		return this;
	}

	public String getUser() {
		return user;
	}

	public SCMCommit setUser(String user) {
		this.user = user;
		return this;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public SCMCommit setUserEmail(String userEmail) {
		this.userEmail = userEmail;
		return this;
	}

	public String getRevId() {
		return revId;
	}

	public SCMCommit setRevId(String revId) {
		this.revId = revId;
		return this;
	}

	public String getParentRevId() {
		return parentRevId;
	}

	public SCMCommit setParentRevId(String parentRevId) {
		this.parentRevId = parentRevId;
		return this;
	}

	public String getComment() {
		return comment;
	}

	public SCMCommit setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public List<SCMChange> getChanges() {
		return changes;
	}

	public SCMCommit setChanges(List<SCMChange> changes) {
		this.changes = changes;
		return this;
	}
}
