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
}