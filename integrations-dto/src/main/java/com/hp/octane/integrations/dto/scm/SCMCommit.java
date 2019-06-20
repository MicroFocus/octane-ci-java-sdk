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
 *
 */

package com.hp.octane.integrations.dto.scm;

import com.hp.octane.integrations.dto.DTOBase;

import java.io.Serializable;
import java.util.List;

/**
 * SCMCommit DTO
 */

public interface SCMCommit extends DTOBase, Serializable {

	Long getTime();

	SCMCommit setTime(Long time);

	String getUser();

	SCMCommit setUser(String user);

	String getUserEmail();

	SCMCommit setUserEmail(String userEmail);

	String getRevId();

	SCMCommit setRevId(String revId);

	String getParentRevId();

	SCMCommit setParentRevId(String parentRevId);

	String getComment();

	SCMCommit setComment(String comment);

	List<SCMChange> getChanges();

	SCMCommit setChanges(List<SCMChange> changes);
}
