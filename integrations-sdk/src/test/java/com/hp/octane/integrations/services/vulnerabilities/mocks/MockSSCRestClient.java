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
package com.hp.octane.integrations.services.vulnerabilities.mocks;

import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.services.rest.SSCRestClient;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.params.HttpParams;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class MockSSCRestClient implements SSCRestClient {

	private int reqCounter = 0;
	private List<String> responseList;

	static class DummyStatusLine implements StatusLine {

		@Override
		public ProtocolVersion getProtocolVersion() {
			return null;
		}

		@Override
		public int getStatusCode() {
			return 200;
		}

		@Override
		public String getReasonPhrase() {
			return null;
		}
	}

	static class DummyEntity implements HttpEntity {

		private String content;

		private DummyEntity(String contentString) {
			this.content = contentString;
		}

		@Override
		public boolean isRepeatable() {
			return false;
		}

		@Override
		public boolean isChunked() {
			return false;
		}

		@Override
		public long getContentLength() {
			return 0;
		}

		@Override
		public Header getContentType() {
			return null;
		}

		@Override
		public Header getContentEncoding() {
			return null;
		}

		@Override
		public InputStream getContent() throws UnsupportedOperationException {
			return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		}

		@Override
		public void writeTo(OutputStream outputStream) {
			//
		}

		@Override
		public boolean isStreaming() {
			return false;
		}

		@Override
		public void consumeContent() {
			//
		}
	}

	static class DummyResponse implements CloseableHttpResponse {

		private String response;

		private DummyResponse(String response) {
			this.response = response;
		}

		@Override
		public void close() {
			//
		}

		@Override
		public StatusLine getStatusLine() {
			return new DummyStatusLine();
		}

		@Override
		public void setStatusLine(StatusLine statusLine) {
			//
		}

		@Override
		public void setStatusLine(ProtocolVersion protocolVersion, int i) {
			//
		}

		@Override
		public void setStatusLine(ProtocolVersion protocolVersion, int i, String s) {
			//
		}

		@Override
		public void setStatusCode(int i) throws IllegalStateException {
			//
		}

		@Override
		public void setReasonPhrase(String s) throws IllegalStateException {
			//
		}

		@Override
		public HttpEntity getEntity() {
			return new DummyEntity(response);
		}

		@Override
		public void setEntity(HttpEntity httpEntity) {
			//
		}

		@Override
		public Locale getLocale() {
			return null;
		}

		@Override
		public void setLocale(Locale locale) {
			//
		}

		@Override
		public ProtocolVersion getProtocolVersion() {
			return null;
		}

		@Override
		public boolean containsHeader(String s) {
			return false;
		}

		@Override
		public Header[] getHeaders(String s) {
			return new Header[0];
		}

		@Override
		public Header getFirstHeader(String s) {
			return null;
		}

		@Override
		public Header getLastHeader(String s) {
			return null;
		}

		@Override
		public Header[] getAllHeaders() {
			return new Header[0];
		}

		@Override
		public void addHeader(Header header) {
			//
		}

		@Override
		public void addHeader(String s, String s1) {
			//
		}

		@Override
		public void setHeader(Header header) {
			//
		}

		@Override
		public void setHeader(String s, String s1) {
			//
		}

		@Override
		public void setHeaders(Header[] headers) {
			//
		}

		@Override
		public void removeHeader(Header header) {
			//
		}

		@Override
		public void removeHeaders(String s) {
			//
		}

		@Override
		public HeaderIterator headerIterator() {
			return null;
		}

		@Override
		public HeaderIterator headerIterator(String s) {
			return null;
		}

		@Override
		public HttpParams getParams() {
			return null;
		}

		@Override
		public void setParams(HttpParams httpParams) {
			//
		}
	}

	@Override
	public CloseableHttpResponse sendGetRequest(SSCProjectConfiguration sscProjectConfiguration, String url) {
		return new DummyResponse(responseList.get(reqCounter++));
	}

	public MockSSCRestClient(List<String> responseList) {
		this.responseList = responseList;
	}
}
