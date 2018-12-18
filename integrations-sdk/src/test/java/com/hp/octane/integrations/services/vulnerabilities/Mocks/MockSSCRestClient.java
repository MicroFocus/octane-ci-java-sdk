package com.hp.octane.integrations.services.vulnerabilities.Mocks;

import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.services.rest.SSCRestClient;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.params.HttpParams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class MockSSCRestClient implements SSCRestClient {

    int reqCounter = 0;
    List<String> responseList;

    static class DummyStatusLine implements StatusLine{

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

    static class DummyEntity implements HttpEntity{

        String content;
        public DummyEntity(String contentString){
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
        public InputStream getContent() throws IOException, UnsupportedOperationException {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void writeTo(OutputStream outputStream) throws IOException {

        }

        @Override
        public boolean isStreaming() {
            return false;
        }

        @Override
        public void consumeContent() throws IOException {

        }
    }

    static class DummyResponse implements CloseableHttpResponse{

        private String response;

        public DummyResponse(String response){

            this.response = response;
        }
        @Override
        public void close() throws IOException {

        }

        @Override
        public StatusLine getStatusLine() {
            return new DummyStatusLine();
        }

        @Override
        public void setStatusLine(StatusLine statusLine) {

        }

        @Override
        public void setStatusLine(ProtocolVersion protocolVersion, int i) {

        }

        @Override
        public void setStatusLine(ProtocolVersion protocolVersion, int i, String s) {

        }

        @Override
        public void setStatusCode(int i) throws IllegalStateException {

        }

        @Override
        public void setReasonPhrase(String s) throws IllegalStateException {

        }

        @Override
        public HttpEntity getEntity() {

            return new DummyEntity(response);
        }

        @Override
        public void setEntity(HttpEntity httpEntity) {

        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public void setLocale(Locale locale) {

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

        }

        @Override
        public void addHeader(String s, String s1) {

        }

        @Override
        public void setHeader(Header header) {

        }

        @Override
        public void setHeader(String s, String s1) {

        }

        @Override
        public void setHeaders(Header[] headers) {

        }

        @Override
        public void removeHeader(Header header) {

        }

        @Override
        public void removeHeaders(String s) {

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

        }
    }

    @Override
    public CloseableHttpResponse sendGetRequest(SSCProjectConfiguration sscProjectConfiguration, String url) {
        return new DummyResponse(responseList.get(reqCounter++));
    }
    public MockSSCRestClient(List<String> responseList){

        this.responseList = responseList;
    }

}
