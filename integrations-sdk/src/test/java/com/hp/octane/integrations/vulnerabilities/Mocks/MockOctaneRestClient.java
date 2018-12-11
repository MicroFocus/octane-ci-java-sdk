package com.hp.octane.integrations.vulnerabilities.Mocks;

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.services.rest.OctaneRestClient;

import java.io.IOException;
import java.util.Map;

public class MockOctaneRestClient implements OctaneRestClient {

    private final String response;
    private final int responseCode;

    public MockOctaneRestClient(String response, int responseCode){
        this.response = response;
        this.responseCode = responseCode;
    }

    @Override
    public OctaneResponse execute(OctaneRequest request) throws IOException {
        return new DummyResponse(this.response, this.responseCode);
    }

    @Override
    public OctaneResponse execute(OctaneRequest request, OctaneConfiguration configuration) throws IOException {

        return new DummyResponse(this.response, this.responseCode);
    }

    @Override
    public void shutdown() {

    }
    public static class DummyResponse implements OctaneResponse {
        private final String response;
        private final int responseCode;

        public DummyResponse(String response, int responseCode){

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
    }
}
