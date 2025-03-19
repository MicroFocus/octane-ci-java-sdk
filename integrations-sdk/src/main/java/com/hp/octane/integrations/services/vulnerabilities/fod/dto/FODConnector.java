/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
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
package com.hp.octane.integrations.services.vulnerabilities.fod.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Predicate;

/**
 * Created by hijaziy on 7/30/2017.
 */
public class FODConnector implements FODSource {

	private static final Logger logger = LogManager.getLogger(FODConnector.class);
	private CloseableHttpClient httpClient;
	private String access_token;
	private long accessTokenTime;

	private final FODConfig fodConfig;

	public FODConnector(FODConfig fodConfig) {
		this.fodConfig = fodConfig;

	}

	public void initConnection(OctaneSDK.SDKServicesConfigurer configurer) {
		logger.debug("init FOD connector");
		try {
			CIProxyConfiguration proxyConfiguration = configurer.pluginServices.getProxyConfiguration(new URL(this.fodConfig.authURLApi));
			if (proxyConfiguration != null) {
				logger.warn("FOD connection needs proxy");
				HttpClientBuilder clientBuilder = HttpClients.custom();
				String proxyHost = proxyConfiguration.getHost();
				Integer proxyPortNumber = proxyConfiguration.getPort();

				clientBuilder.setProxy(new HttpHost(proxyHost, proxyPortNumber));
				httpClient = clientBuilder.build();
			} else {
				logger.warn("FOD connection does not need proxy");
				httpClient = HttpClients.createDefault();
			}
			getAccessToken();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public <T extends FODEntityCollection> T getAllFODEntities(String rawURL, Class<T> targetClass, Predicate<T> whenToStopFetch) {

		try {
			T fetchedEnts = targetClass.newInstance();
			boolean allIsFetched = false;
			boolean shouldStopFetching = false;
			while (!allIsFetched) {
				try {
					int offset = fetchedEnts.items.size();
					String indexedURL = addOffsetToURL(rawURL, offset);
					String rawResponse = getRawResponseFromFOD(indexedURL);
					//Deserialize.
					T entityCollection = new ObjectMapper().readValue(rawResponse,
							TypeFactory.defaultInstance().constructType((fetchedEnts).getClass()));

					if (whenToStopFetch != null) {
						shouldStopFetching = whenToStopFetch.test(entityCollection);
					}

					fetchedEnts.items.addAll(entityCollection.items);
					fetchedEnts.totalCount = fetchedEnts.items.size();
					allIsFetched = (fetchedEnts.totalCount == entityCollection.totalCount) || shouldStopFetching;
				} catch (IOException e) {
					e.printStackTrace();
					break;
				} catch (PermanentException e){
					logger.error(e.getMessage());
				}
			}
			return fetchedEnts;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public <T> T getSpeceficFODEntity(String rawURL, Class<T> targetClass) {

		try {
			T fetchedEntityInstance = targetClass.newInstance();

			String rawResponse = getRawResponseFromFOD(rawURL);
			//Deserialize.
			T entityFetched = new ObjectMapper().readValue(rawResponse,
					TypeFactory.defaultInstance().constructType((fetchedEntityInstance).getClass()));

			return entityFetched;
		} catch (PermanentException e){
			logger.error(e.getMessage());
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private String addOffsetToURL(String rawURL, int offset) {

		String offsetDirective = "offset=" + String.valueOf(offset);
		if (!rawURL.contains("?")) {
			return rawURL + "?" + offsetDirective;
		}
		return rawURL + "&" + offsetDirective;
	}

	private String getRawResponseFromFOD(String url) {
		//URL encode
		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeader("Authorization", "Bearer " + getUpdatedAccessToken());
		httpGet.setHeader("Accept", "application/json");
		httpGet.setHeader("Cookie", "__zlcmid=hTgaa94NtAdw5T; FoD=1725197834.36895.0000");
		httpGet.setHeader("Accept-Encoding", "gzip, deflate, br");
		CloseableHttpResponse response = null;
		try {
			logger.debug("sending the request : "+url);
			response = httpClient.execute(httpGet);
			if (response.getStatusLine().getStatusCode()>300 ) {
				if(response.getStatusLine().getStatusCode()==401){
					throw new PermanentException("Cannot authenticate , the user is Unauthorized , make sure you are using the correct role for the user (): " +response.getStatusLine().getReasonPhrase());
				}
				if(response.getStatusLine().getStatusCode() == 503){
					throw new TemporaryException("Service Unavailable ,  retry");
				}
				if(response.getStatusLine().getStatusCode() == 500){
					throw new PermanentException("Internal FOD Server Error 500, failed to get vulnerability, httpGet: " + httpGet + "  unexpected return response : " +response.getStatusLine().getReasonPhrase());
				}
				throw new PermanentException("unexpected return response : " +response.getStatusLine().getReasonPhrase());
			}
			return isToString(response.getEntity().getContent());
		} catch (IOException e) {
			throw new PermanentException(e);
		} finally {
			if (response != null) {
				EntityUtils.consumeQuietly(response.getEntity());
				HttpClientUtils.closeQuietly(response);
			}
		}
	}

	private String getUpdatedAccessToken() {
		long currentTime = new Date().getTime();
		long delta = getTimeToRefreshToken();
		if (currentTime - accessTokenTime >= delta) {
			getAccessToken();
		}
		return access_token;
	}

	private void getAccessToken() {

		HttpPost post = new HttpPost(fodConfig.authURLApi);
		HttpEntity content = new StringEntity(fodConfig.getAuthBody(), ContentType.APPLICATION_FORM_URLENCODED);

		post.setEntity(content);
		CloseableHttpResponse response = null;
		try {

			logger.debug("inside getAccessToken():before Post ");
			response = httpClient.execute(post);
			logger.debug("inside getAccessToken():after Post, got status : "+response.getStatusLine().getStatusCode());
			if (response.getStatusLine().getStatusCode() != 200 &&
					response.getStatusLine().getStatusCode() != 201) {
				logger.error("inside getAccessToken():after Post, got status : "+response.getStatusLine().getStatusCode());
				throw new PermanentException("Cannot authenticate:" +response.getStatusLine().getReasonPhrase());
			}

			String secToken = isToString(response.getEntity().getContent());
			HashMap secTokeAsMap = new ObjectMapper().readValue(secToken, HashMap.class);
			access_token = secTokeAsMap.get("access_token").toString();
			accessTokenTime = new Date().getTime();
			logger.debug("inside getAccessToken():after Post ");

		} catch (IOException e) {
			logger.error("inside getAccessToken():After Post inside exception ",e);
			e.printStackTrace();
			if(response != null) {
				EntityUtils.consumeQuietly(response.getEntity());
				HttpClientUtils.closeQuietly(response);
			}
			throw new PermanentException("Cannot authenticate:" + e.getMessage());
		} finally {
			if (response != null) {
				EntityUtils.consumeQuietly(response.getEntity());
				HttpClientUtils.closeQuietly(response);
			}
		}
	}

	static String isToString(InputStream is) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		while ((length = is.read(buffer)) != -1) {
			result.write(buffer, 0, length);
		}
		return result.toString(Charset.forName("UTF-8").name());
	}

	@Override
	public String getEntitiesURL() {
		return fodConfig.entitiesURL;

	}

	public static Long getTimeToRefreshToken() {
		//String fortifyPollDelayStr = siteParamsService.getParam(FODConstants.TIME_TO_REFRESH_FORTIFY_TOKEN_MINUTES);
		return 60L * 1000 * 60;
	}

}
