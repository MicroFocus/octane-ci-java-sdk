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
package com.hp.octane.integrations.dto.scm;

import com.hp.octane.integrations.dto.DTOBase;

import java.io.Serializable;
import java.util.List;

/**
 * SCMCommit DTO
 */
@SuppressWarnings({"unused"})
public interface PullRequest extends DTOBase, Serializable {

	String getId();

	PullRequest setId(String id);

	String getState();

	PullRequest setState(String state);

	String getTitle();

	PullRequest setTitle(String title);

	String getDescription();

	PullRequest setDescription(String description);

	Long getCreatedTime();

	PullRequest setCreatedTime(Long createdTime);

	Long getUpdatedTime();

	PullRequest setUpdatedTime(Long updatedTime);

	Long getMergedTime();

	PullRequest setMergedTime(Long mergedTime);

	Long getClosedTime();

	PullRequest setClosedTime(Long closedTime);

	String getAuthorName();

	PullRequest setAuthorName(String authorName);

	String getAuthorEmail();

	PullRequest setAuthorEmail(String authorEmail);

	SCMRepository getSourceRepository();

	PullRequest setSourceRepository(SCMRepository sourceScmRepository);

	SCMRepository getTargetRepository();

	PullRequest setTargetRepository(SCMRepository targetScmRepository);

	String getSelfUrl();

	PullRequest setSelfUrl(String selfUrl);

	List<SCMCommit> getCommits();

	PullRequest setCommits(List<SCMCommit> commits);

	boolean isMerged();

	PullRequest setIsMerged(boolean isMerged);

}
