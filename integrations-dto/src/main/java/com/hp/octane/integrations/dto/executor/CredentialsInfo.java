package com.hp.octane.integrations.dto.executor;

import com.hp.octane.integrations.dto.DTOBase;

/**
 * Created by shitritn on 30/04/2017.
 */
public interface CredentialsInfo extends DTOBase {

    String getUsername();

    CredentialsInfo setUsername(String username);

    String getPassword();

    CredentialsInfo setPassword(String password);

    String getCredentialId();

    CredentialsInfo setCredentialId(String credentialId);
}
