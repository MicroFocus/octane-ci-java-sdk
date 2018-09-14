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
 */

package com.hp.octane.integrations.services.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.vulnerabilities.SSCFortifyConfigurations;
import com.hp.octane.integrations.services.vulnerabilities.ssc.AuthToken;
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

public class SSCClientImpl implements SSCClient {

	private static final int MAX_TOTAL_CONNECTIONS = 20;

	private final CloseableHttpClient httpClient;
	private AuthToken.AuthTokenData authTokenData;

	SSCClientImpl() {
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

	public CloseableHttpResponse sendGetRequest(SSCFortifyConfigurations sscFortifyConfigurations, String url) {
		HttpGet request = new HttpGet(url);
		request.addHeader("Authorization", "FortifyToken " +
				getToken(sscFortifyConfigurations, false));
		request.addHeader("Accept", "application/json");
		request.addHeader("Host", getNetHost(sscFortifyConfigurations.serverURL));

		CloseableHttpResponse response;
		try {
			response = httpClient.execute(request);
			//401. Access..
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				request.removeHeaders("Authorization");
				request.addHeader("Authorization", "FortifyToken " +
						getToken(sscFortifyConfigurations, true));
				response = httpClient.execute(request);
			}
			return response;
		} catch (IOException e) {
			throw new TemporaryException(e);
		} catch (Exception e) {
			throw new PermanentException(e);
		}
	}

	private String getToken(SSCFortifyConfigurations sscCfgs, boolean forceRenew) {
		if (forceRenew || authTokenData == null) {
			authTokenData = sendReqAuth(sscCfgs);
		}
		return authTokenData.token;
	}


	private AuthToken.AuthTokenData sendReqAuth(SSCFortifyConfigurations sscCfgs) {
		//"/{SSC Server Context}/api/v1"
		//String url = "http://" + serverURL + "/ssc/api/v1/projects?q=id:2743&fulltextsearch=true";
		String url = sscCfgs.serverURL + "/api/v1/tokens";
		HttpPost request = new HttpPost(url);
		request.addHeader("Authorization", sscCfgs.baseToken);
		request.addHeader("Accept", "application/json");
		request.addHeader("Host", getNetHost(sscCfgs.serverURL));
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
				return authToken.data;
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
		String prefix = "http://";
		int indexOfStart = serverURL.toLowerCase().indexOf(prefix) + prefix.length();
		int indexOfEnd = serverURL.lastIndexOf("/");
		if (indexOfEnd < 0) {
			return serverURL.substring(indexOfStart);
		}
		return serverURL.substring(indexOfStart, indexOfEnd);
	}

	private boolean succeeded(int statusCode) {
		return statusCode == 200 || statusCode == 201;
	}
}
