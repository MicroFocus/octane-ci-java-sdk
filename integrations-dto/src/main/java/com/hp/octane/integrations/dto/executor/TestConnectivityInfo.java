package com.hp.octane.integrations.dto.executor;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.scm.SCMRepository;

/**
 * Created by shitritn on 4/3/2017.
 */
public interface TestConnectivityInfo extends DTOBase {
    SCMRepository getScmRepository();

    TestConnectivityInfo setScmRepository(SCMRepository scmRepository);

    String getUsername();

    TestConnectivityInfo setUsername(String username);

    String getPassword();

    TestConnectivityInfo setPassword(String password);

    String getCredentialsId();

    TestConnectivityInfo setCredentialsId(String CredentialsId);

}
