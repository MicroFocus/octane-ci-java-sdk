package com.hp.octane.integrations.dto.securityscans;

import com.hp.octane.integrations.dto.DTOBase;

public interface FodServerConfiguration extends DTOBase {

    String getApiUrl();
    FodServerConfiguration setApiUrl(String sscUrl);

    String getClientId();
    FodServerConfiguration setClientId(String sscBaseAuthToken);

    String getClientSecret();
    FodServerConfiguration setClientSecret(String sscBaseAuthToken);

    long getMaxPollingTimeoutHours();
    FodServerConfiguration setMaxPollingTimeoutHours(long maxPollingTimeoutHours);

    boolean isValid();

    FodServerConfiguration setBaseUrl(String baseUrl);
    String getBaseUrl();
}
