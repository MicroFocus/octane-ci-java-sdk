package com.hp.octane.integrations.api;

import com.hp.octane.integrations.services.vulnerabilities.SSCFortifyConfigurations;
import com.hp.octane.integrations.services.vulnerabilities.ssc.AuthToken;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;

public interface SSCClient {
    CloseableHttpResponse sendGetRequest(SSCFortifyConfigurations sscFortifyConfigurations, String url);
}
