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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.spi.CIPluginServices;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.http.Header;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.io.IOException;

import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * REST Client default implementation
 */

final class OctaneRestClientImpl implements OctaneRestClient {
	private static final Logger logger = LogManager.getLogger(OctaneRestClientImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private static final Set<Integer> AUTHENTICATION_ERROR_CODES = Stream.of(HttpStatus.SC_UNAUTHORIZED).collect(Collectors.toSet());
	private static final String CLIENT_TYPE_HEADER = "HPECLIENTTYPE";
	private static final String CLIENT_TYPE_VALUE = "HPE_CI_CLIENT";
	private static final String LWSSO_COOKIE_NAME = "LWSSO_COOKIE_KEY";
	private static final String AUTHENTICATION_URI = "authentication/sign_in";
	private static final int MAX_TOTAL_CONNECTIONS = 20;

	private final CIPluginServices pluginServices;
	private final CloseableHttpClient httpClient;
	private final List<HttpUriRequest> ongoingRequests = new LinkedList<>();
	private final Object REQUESTS_LIST_LOCK = new Object();

	private Cookie LWSSO_TOKEN = null;

	OctaneRestClientImpl(OctaneSDK.SDKServicesConfigurer configurer) {
		if (configurer == null || configurer.pluginServices == null) {
			throw new IllegalArgumentException("invalid configurer");
		}

		pluginServices = configurer.pluginServices;

		SSLContext sslContext = SSLContexts.createSystemDefault();
		HostnameVerifier hostnameVerifier = new CustomHostnameVerifier();
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
	public OctaneResponse execute(OctaneRequest request) throws IOException {
		return executeRequest(request, pluginServices.getOctaneConfiguration());
	}

	@Override
	public OctaneResponse execute(OctaneRequest request, OctaneConfiguration configuration) throws IOException {
		return executeRequest(request, configuration);
	}

	@Override
	public void shutdown() {
		logger.info("starting REST client shutdown sequence...");
		abortAllRequests();
		logger.info("closing the client...");
		HttpClientUtils.closeQuietly(httpClient);
		logger.info("REST client shutdown done");
	}

	void notifyConfigurationChange() {
		abortAllRequests();
	}

	private void abortAllRequests() {
		logger.info("aborting " + ongoingRequests.size() + " request/s...");
		synchronized (REQUESTS_LIST_LOCK) {
			LWSSO_TOKEN = null;
			for (HttpUriRequest request : ongoingRequests) {
				logger.info("\taborting " + request);
				request.abort();
			}
		}
	}

	private OctaneResponse executeRequest(OctaneRequest request, OctaneConfiguration configuration) throws IOException {
		OctaneResponse result;
		HttpClientContext context;
		HttpUriRequest uriRequest = null;
		HttpResponse httpResponse = null;
		OctaneResponse loginResponse;
		if (LWSSO_TOKEN == null) {
			logger.info("initial login");
			loginResponse = login(configuration);
			if (loginResponse.getStatus() != 200) {
				logger.error("failed on initial login, status " + loginResponse.getStatus());
				return loginResponse;
			}
		}

		try {
			//  we are running this loop either once or twice: once - regular flow, twice - when retrying after re-login attempt
			for (int i = 0; i < 2; i++) {
				uriRequest = createHttpRequest(request);
				context = createHttpContext(request.getUrl(), false);
				synchronized (REQUESTS_LIST_LOCK) {
					ongoingRequests.add(uriRequest);
				}
				httpResponse = httpClient.execute(uriRequest, context);
				synchronized (REQUESTS_LIST_LOCK) {
					ongoingRequests.remove(uriRequest);
				}

				if (AUTHENTICATION_ERROR_CODES.contains(httpResponse.getStatusLine().getStatusCode())) {
					logger.info("doing RE-LOGIN due to status " + httpResponse.getStatusLine().getStatusCode() + " received while calling " + request.getUrl());
					EntityUtils.consumeQuietly(httpResponse.getEntity());
					HttpClientUtils.closeQuietly(httpResponse);
					loginResponse = login(configuration);
					if (loginResponse.getStatus() != 200) {
						logger.error("failed to RE-LOGIN with status " + loginResponse.getStatus() + ", won't attempt the original request anymore");
						return loginResponse;
					} else {
						logger.info("re-attempting the original request (" + request.getUrl() + ") having successful RE-LOGIN");
					}
				} else {
					refreshSecurityToken(context, false);
					break;
				}
			}

			result = createNGAResponse(httpResponse);
		} catch (IOException ioe) {
			logger.debug("failed executing " + request, ioe);
			throw ioe;
		} finally {
			if (uriRequest != null && ongoingRequests.contains(uriRequest)) {
				synchronized (REQUESTS_LIST_LOCK) {
					ongoingRequests.remove(uriRequest);
				}
			}
			if (httpResponse != null) {
				EntityUtils.consumeQuietly(httpResponse.getEntity());
				HttpClientUtils.closeQuietly(httpResponse);
			}
		}

		return result;
	}

	/**
	 * This method should be the ONLY mean that creates Http Request objects
	 *
	 * @param octaneRequest Request data as it is maintained in Octane related flavor
	 * @return pre-configured HttpUriRequest
	 */
	private HttpUriRequest createHttpRequest(OctaneRequest octaneRequest) {
		HttpUriRequest request;
		RequestBuilder requestBuilder;

		//  create base request by METHOD
		if (octaneRequest.getMethod().equals(HttpMethod.GET)) {
			requestBuilder = RequestBuilder.get(octaneRequest.getUrl());
		} else if (octaneRequest.getMethod().equals(HttpMethod.DELETE)) {
			requestBuilder = RequestBuilder.delete(octaneRequest.getUrl());
		} else if (octaneRequest.getMethod().equals(HttpMethod.POST)) {
			requestBuilder = RequestBuilder.post(octaneRequest.getUrl());
			requestBuilder.addHeader(new BasicHeader(RestService.CONTENT_ENCODING_HEADER, RestService.GZIP_ENCODING));
			requestBuilder.setEntity(new GzipCompressingEntity(new InputStreamEntity(octaneRequest.getBody(), ContentType.APPLICATION_JSON)));
		} else if (octaneRequest.getMethod().equals(HttpMethod.PUT)) {
			requestBuilder = RequestBuilder.put(octaneRequest.getUrl());
			requestBuilder.addHeader(new BasicHeader(RestService.CONTENT_ENCODING_HEADER, RestService.GZIP_ENCODING));
			requestBuilder.setEntity(new GzipCompressingEntity(new InputStreamEntity(octaneRequest.getBody(), ContentType.APPLICATION_JSON)));
		} else {
			throw new RuntimeException("HTTP method " + octaneRequest.getMethod() + " not supported");
		}

		//  set custom headers
		if (octaneRequest.getHeaders() != null) {
			for (Map.Entry<String, String> e : octaneRequest.getHeaders().entrySet()) {
				requestBuilder.setHeader(e.getKey(), e.getValue());
			}
		}

		//  set system headers
		requestBuilder.setHeader(CLIENT_TYPE_HEADER, CLIENT_TYPE_VALUE);

		request = requestBuilder.build();
		return request;
	}

	private HttpClientContext createHttpContext(String requestUrl, boolean isLoginRequest) {
		HttpClientContext context = HttpClientContext.create();
		context.setCookieStore(new BasicCookieStore());

		//  add security token if needed
		if (!isLoginRequest) {
			context.getCookieStore().addCookie(LWSSO_TOKEN);
		}

		//  prepare request config
		RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
				.setCookieSpec(CookieSpecs.STANDARD);

		//  configure proxy if needed
		URL parsedUrl = CIPluginSDKUtils.parseURL(requestUrl);
		CIProxyConfiguration proxyConfiguration = pluginServices.getProxyConfiguration(parsedUrl);
		if (proxyConfiguration != null) {
			logger.debug("proxy will be used with the following setup: " + proxyConfiguration);
			HttpHost proxyHost = new HttpHost(proxyConfiguration.getHost(), proxyConfiguration.getPort());

			if (proxyConfiguration.getUsername() != null && !proxyConfiguration.getUsername().isEmpty()) {
				AuthScope authScope = new AuthScope(proxyHost);
				Credentials credentials = new UsernamePasswordCredentials(proxyConfiguration.getUsername(), proxyConfiguration.getPassword());
				CredentialsProvider credentialsProvider = new SystemDefaultCredentialsProvider();
				credentialsProvider.setCredentials(authScope, credentials);
				context.setCredentialsProvider(credentialsProvider);
			}
			requestConfigBuilder.setProxy(proxyHost);
		}

		context.setRequestConfig(requestConfigBuilder.build());
		return context;
	}

	private void refreshSecurityToken(HttpClientContext context, boolean mustPresent) {
		boolean securityTokenRefreshed = false;
		for (Cookie cookie : context.getCookieStore().getCookies()) {
			if (LWSSO_COOKIE_NAME.equals(cookie.getName()) && (LWSSO_TOKEN == null || cookie.getValue().compareTo(LWSSO_TOKEN.getValue()) != 0)) {
				((BasicClientCookie) cookie).setPath("/");
				LWSSO_TOKEN = cookie;
				securityTokenRefreshed = true;
				break;
			}
		}

		if (securityTokenRefreshed) {
			logger.info("successfully refreshed security token");
		} else if (mustPresent) {
			logger.error("security token expected but NOT found (domain attribute configured wrongly?)");
		}
	}

	private OctaneResponse createNGAResponse(HttpResponse response) throws IOException {
		OctaneResponse octaneResponse = dtoFactory.newDTO(OctaneResponse.class)
				.setStatus(response.getStatusLine().getStatusCode());
		if (response.getEntity() != null) {
			octaneResponse.setBody(CIPluginSDKUtils.inputStreamToUTF8String(response.getEntity().getContent()));
		}
		if (response.getAllHeaders() != null && response.getAllHeaders().length > 0) {
			Map<String, String> mapHeaders = new HashMap<>();
			for (Header header : response.getAllHeaders()) {
				mapHeaders.put(header.getName(), header.getValue());
			}
			octaneResponse.setHeaders(mapHeaders);
		}
		return octaneResponse;
	}

	private OctaneResponse login(OctaneConfiguration config) throws IOException {
		OctaneResponse result;
		HttpResponse response = null;

		try {
			HttpUriRequest loginRequest = buildLoginRequest(config);
			HttpClientContext context = createHttpContext(loginRequest.getURI().toString(), true);
			response = httpClient.execute(loginRequest, context);

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				refreshSecurityToken(context, true);
			} else {
				logger.warn("failed to login to " + config + "; response status: " + response.getStatusLine().getStatusCode());
			}
			result = createNGAResponse(response);
		} catch (IOException ioe) {
			logger.debug("failed to login to " + config, ioe);
			throw ioe;
		} finally {
			if (response != null) {
				EntityUtils.consumeQuietly(response.getEntity());
				HttpClientUtils.closeQuietly(response);
			}
		}

		return result;
	}

	private HttpUriRequest buildLoginRequest(OctaneConfiguration config) throws IOException {
		HttpUriRequest loginRequest;
		try {
			LoginApiBody loginApiBody = new LoginApiBody(config.getApiKey(), config.getSecret());
			StringEntity loginApiJson = new StringEntity(CIPluginSDKUtils.getObjectMapper().writeValueAsString(loginApiBody), ContentType.APPLICATION_JSON);
			RequestBuilder requestBuilder = RequestBuilder.post(config.getUrl() + "/" + AUTHENTICATION_URI)
					.setHeader(CLIENT_TYPE_HEADER, CLIENT_TYPE_VALUE)
					.setEntity(loginApiJson);
			loginRequest = requestBuilder.build();
			return loginRequest;
		} catch (JsonProcessingException jpe) {
			throw new IOException("failed to serialize login content", jpe);
		}
	}

	public static final class CustomHostnameVerifier implements HostnameVerifier {
		private final HostnameVerifier defaultVerifier = new DefaultHostnameVerifier();

		public boolean verify(String host, SSLSession sslSession) {
			boolean result = defaultVerifier.verify(host, sslSession);
			if (!result) {
				try {
					Certificate[] ex = sslSession.getPeerCertificates();
					X509Certificate x509 = (X509Certificate) ex[0];
					Collection<List<?>> altNames = x509.getSubjectAlternativeNames();
					for (List<?> namePair : altNames) {
						if (namePair != null &&
								namePair.size() > 1 &&
								namePair.get(1) instanceof String &&
								"*.saas.hp.com".equals(namePair.get(1))) {
							result = true;
							break;
						}
					}
				} catch (CertificateParsingException cpe) {
					logger.error("failed to parse certificate", cpe);       //  result will remain false
				} catch (SSLException ssle) {
					logger.error("failed to handle certificate", ssle);     //  result will remain false
				}
			}
			return result;
		}
	}

	@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
	private static final class LoginApiBody {
		private final String client_id;
		private final String client_secret;

		private LoginApiBody(String client_id, String client_secret) {
			this.client_id = client_id;
			this.client_secret = client_secret;
		}

		@Override
		public String toString() {
			return "LoginApiBody {" +
					"client_id: " + client_id +
					", client_secret: " + client_secret + "}";
		}
	}
}
