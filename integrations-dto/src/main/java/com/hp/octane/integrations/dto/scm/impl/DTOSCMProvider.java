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

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.DTOInternalProviderBase;
import com.hp.octane.integrations.dto.scm.*;

/**
 * SCM related DTOs definitions provider
 */

public final class DTOSCMProvider extends DTOInternalProviderBase {

	public DTOSCMProvider(DTOFactory.DTOConfiguration configuration) {
		super(configuration);
		dtoPairs.put(SCMChange.class, SCMChangeImpl.class);
		dtoPairs.put(SCMCommit.class, SCMCommitImpl.class);
		dtoPairs.put(SCMRepository.class, SCMRepositoryImpl.class);
		dtoPairs.put(SCMRepositoryLinks.class, SCMRepositoryLinksImpl.class);
		dtoPairs.put(SCMData.class, SCMDataImpl.class);
		dtoPairs.put(SCMFileBlame.class, SCMFileBlameImpl.class);
		dtoPairs.put(PullRequest.class, PullRequestImpl.class);
		dtoPairs.put(Branch.class, BranchImpl.class);
	}

	protected <T extends DTOBase> T instantiateDTO(Class<T> targetType) throws InstantiationException, IllegalAccessException {
		T result = null;
		if (dtoPairs.containsKey(targetType)) {
			result = (T) dtoPairs.get(targetType).newInstance();
		}
		return result;
	}
}
