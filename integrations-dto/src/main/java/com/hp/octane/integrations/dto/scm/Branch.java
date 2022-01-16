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

/**
 * SCMCommit DTO
 */
@SuppressWarnings({"unused"})
public interface Branch extends DTOBase, Serializable {

    String getName();

    Branch setName(String name);

    Boolean getIsMerged();

    Branch setIsMerged(boolean isMerged);

    String getLastCommitSHA();

    Branch setLastCommitSHA(String lastCommitSHA);

    String getLastCommitUrl();

    Branch setLastCommitUrl(String lastCommitUrl);

    Long getLastCommitTime();

    Branch setLastCommitTime(Long lastCommitTime);

    String getLastCommiterName();

    Branch setLastCommiterName(String lastCommiterName);

    String getLastCommiterEmail();

    Branch setLastCommiterEmail(String lastCommiterEmail);

    boolean isPartial();

    Branch setPartial(boolean partial);

    String getOctaneId();

    Branch setOctaneId(String octaneId);
}
