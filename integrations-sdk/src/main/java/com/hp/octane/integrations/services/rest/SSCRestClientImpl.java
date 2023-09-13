/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
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

package com.hp.octane.integrations.services.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.AuthToken;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class SSCRestClientImpl implements SSCRestClient {

    private static final int MAX_TOTAL_CONNECTIONS = 20;

    private final CloseableHttpClient httpClient;
    private AuthToken.AuthTokenData authTokenData;

    SSCRestClientImpl(OctaneSDK.SDKServicesConfigurer configurer) {
        if (configurer == null || configurer.pluginServices == null) {
            throw new IllegalArgumentException("invalid configurer");
        }

        SSLContext sslContext = SSLContexts.createSystemDefault();
        HostnameVerifier hostnameVerifier = new OctaneRestClientImpl.CustomHostnameVerifier();
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_TOTAL_CONNECTIONS);

        HttpClientBuilder clientBuilder = HttpClients.custom()
                .setConnectionManager(connectionManager);

        httpClient = clientBuilder.build();
    }

    @Override
    public CloseableHttpResponse sendGetRequest(SSCProjectConfiguration sscProjectConfiguration, String url) {
        HttpGet request = new HttpGet(url);
        request.addHeader("Authorization", "FortifyToken " +
                getToken(sscProjectConfiguration, false));
        request.addHeader("Accept", "application/json");
        request.addHeader("Host", getNetHost(sscProjectConfiguration.getSSCUrl()));

        CloseableHttpResponse response;
        try {
            response = httpClient.execute(request);
            //401. Access..
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                request.removeHeaders("Authorization");
                request.addHeader("Authorization", "FortifyToken " +
                        getToken(sscProjectConfiguration, true));
                response = httpClient.execute(request);
            }
            return response;
        } catch (IOException e) {
            throw new TemporaryException(e);
        } catch (Exception e) {
            throw new PermanentException(e);
        }
    }

    private String getToken(SSCProjectConfiguration sscProjectConfiguration, boolean forceRenew) {
        if (forceRenew || authTokenData == null) {
            authTokenData = sendReqAuth(sscProjectConfiguration);
        }
        return authTokenData.token;
    }

    private AuthToken.AuthTokenData sendReqAuth(SSCProjectConfiguration sscProjectConfiguration) {
        //"/{SSC Server Context}/api/v1"
        //String url = "http://" + serverURL + "/ssc/api/v1/projects?q=id:2743&fulltextsearch=true";
        String url = sscProjectConfiguration.getSSCUrl() + "/api/v1/tokens";
        HttpPost request = new HttpPost(url);
        request.addHeader("Authorization", sscProjectConfiguration.getSSCBaseAuthToken());
        request.addHeader("Accept", "application/json");
        request.addHeader("Host", getNetHost(sscProjectConfiguration.getSSCUrl()));
        request.addHeader("Content-Type", "application/json;charset=UTF-8");

        String body = "{\"type\": \"UnifiedLoginToken\"}";
        CloseableHttpResponse response = null;
        try {
            HttpEntity entity = new ByteArrayEntity(body.getBytes(StandardCharsets.UTF_8));
            request.setEntity(entity);
            response = httpClient.execute(request);
            if (succeeded(response.getStatusLine().getStatusCode())) {
                String toString = CIPluginSDKUtils.inputStreamToUTF8String(response.getEntity().getContent());
                AuthToken authToken = new ObjectMapper().readValue(toString, TypeFactory.defaultInstance().constructType(AuthToken.class));
                return authToken.getData();
            } else {
                throw new PermanentException("Couldn't Authenticate SSC user, need to check SSC configuration in Octane plugin");
            }
        } catch (Throwable t) {
            throw new PermanentException(t);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
                HttpClientUtils.closeQuietly(response);
            }
        }
    }

    private String getNetHost(String serverURL) {
        //http://myd-vma00564.swinfra.net:8180/ssc
        String prefix = "://";
        int indexOfStart = serverURL.toLowerCase().indexOf(prefix) + prefix.length();
        int indexOfEnd = serverURL.lastIndexOf("/");
        if (indexOfEnd < 0 || indexOfEnd <= indexOfStart) {
            return serverURL.substring(indexOfStart);
        }
        return serverURL.substring(indexOfStart, indexOfEnd);
    }

    private boolean succeeded(int statusCode) {
        return statusCode == 200 || statusCode == 201;
    }
}
