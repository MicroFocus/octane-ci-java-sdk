package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOBase;

import java.io.Serializable;

public interface OctaneConnectivityStatus extends DTOBase , Serializable {

    OctaneConnectivityStatus setSupportedSdkVersion(String supportedSdkVersion);

    String getSupportedSdkVersion();

    OctaneConnectivityStatus setOctaneVersion(String octaneVersion);

    String getOctaneVersion();
}
