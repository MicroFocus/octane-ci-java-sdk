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

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.services.rest.OctaneRestClient;

import java.util.Map;

public class MockOctaneRestClient implements OctaneRestClient {

    private final String response;
    private final int responseCode;

    public MockOctaneRestClient(String response, int responseCode){
        this.response = response;
        this.responseCode = responseCode;
    }

    @Override
    public OctaneResponse execute(OctaneRequest request) {
        return new DummyResponse(this.response, this.responseCode);
    }

    @Override
    public OctaneResponse execute(OctaneRequest request, OctaneConfiguration configuration) {

        return new DummyResponse(this.response, this.responseCode);
    }

    @Override
    public void shutdown() {

    }

    @Override
    public Map<String, Object> getMetrics() {
        return null;
    }

    public static class DummyResponse implements OctaneResponse {
        private final String response;
        private final int responseCode;

        private DummyResponse(String response, int responseCode){

            this.response = response;
            this.responseCode = responseCode;
        }

        @Override
        public int getStatus() {
            return responseCode;
        }

        @Override
        public OctaneResponse setStatus(int status) {
            return null;
        }

        @Override
        public Map<String, String> getHeaders() {
            return null;
        }

        @Override
        public OctaneResponse setHeaders(Map<String, String> headers) {
            return null;
        }

        @Override
        public String getBody() {
            return response;
        }

        @Override
        public OctaneResponse setBody(String body) {
            return null;
        }

        @Override
        public String getCorrelationId() {
            return null;
        }

        @Override
        public OctaneResponse setCorrelationId(String correlationId) {
            return null;
        }
    }
}
