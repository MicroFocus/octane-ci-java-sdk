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

    TestingToolType getTestingToolType();

    DiscoveryInfo setTestingToolType(TestingToolType testingToolType);

    String getScmRepositoryId();

    void setScmRepositoryId(String scmRepositoryId);

    String getScmRepositoryCredentialsId();

    void setScmRepositoryCredentialsId(String scmRepositoryCredentialsId);
}