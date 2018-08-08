package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOBase;

public interface SonarInfo extends DTOBase {

    String getServerUrl();

    SonarInfo setServerUrl(String serverUrl) ;

    String getServerVersion() ;

    SonarInfo setServerVersion(String serverVersion) ;

    String getServerAuthenticationToken();

    SonarInfo setServerAuthenticationToken(String serverAuthenticationToken) ;




}
