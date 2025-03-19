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
package com.hp.octane.integrations.dto.executor;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.dto.scm.SCMRepository;

/**
 * Created by berkovir on 27/03/2017.
 */
public interface DiscoveryInfo extends DTOBase {

    String getWorkspaceId();

    DiscoveryInfo setWorkspaceId(String workspaceId);

    SCMRepository getScmRepository();

    DiscoveryInfo setScmRepository(SCMRepository scmRepository);

    boolean isForceFullDiscovery();

    DiscoveryInfo setForceFullDiscovery(boolean forceFullDiscovery);

    String getExecutorId();

    DiscoveryInfo setExecutorId(String executorId);

    String getExecutorType() ;

    void setExecutorType(String executorType);

    TestingToolType getTestingToolType();

    DiscoveryInfo setTestingToolType(TestingToolType testingToolType);

    String getScmRepositoryId();

    void setScmRepositoryId(String scmRepositoryId);

    String getScmRepositoryCredentialsId();

    void setScmRepositoryCredentialsId(String scmRepositoryCredentialsId);

    String getExecutorLogicalName();

    DiscoveryInfo setExecutorLogicalName(String executorLogicalName);

	String getConfigurationId();

	void setConfigurationId(String configurationId);

	String getAdditionalData();

    DiscoveryInfo setAdditionalData(String additionalData);
}