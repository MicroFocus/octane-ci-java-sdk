package com.hp.octane.integrations.api;

import com.hp.octane.integrations.services.vulnerabilities.SSCFortifyConfigurations;
import org.apache.http.client.methods.CloseableHttpResponse;

public interface SSCClient {
    CloseableHttpResponse sendGetRequest(SSCFortifyConfigurations sscFortifyConfigurations, String url);
}
