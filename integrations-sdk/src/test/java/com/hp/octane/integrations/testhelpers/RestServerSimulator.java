/*
 * Copyright 2017-2026 Open Text
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
package com.hp.octane.integrations.testhelpers;

import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class RestServerSimulator extends Handler.Abstract {

    private int selectedPort;
    private Server server;
    private List<RequestHandlingRule> handlingRules = new ArrayList<>();
    private List<Request> receivedRequests = new ArrayList<>();

    //  Jetty 12: the request callback must be passed to the async body write, otherwise the response
    //  is finalized before the body is flushed. We expose it to the (callback-less) rule handlers via ThreadLocal.
    private static final ThreadLocal<Callback> CURRENT_CALLBACK = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> RESPONSE_WRITTEN = new ThreadLocal<>();

    /**
     * Writes a response body and completes the current request callback (Jetty 12 async-safe).
     * Rule handlers MUST use this instead of {@code Content.Sink.write(..., Callback.NOOP)} when returning a body.
     */
    public static void writeResponseBody(Response response, String content) {
        Callback cb = CURRENT_CALLBACK.get();
        RESPONSE_WRITTEN.set(Boolean.TRUE);
        Content.Sink.write(response, true, content, cb != null ? cb : Callback.NOOP);
    }

    /**
     * Reads the full request body as a UTF-8 string, transparently gunzipping when Content-Encoding is gzip.
     */
    public static String readRequestBody(Request request) {
        try (InputStream is = Content.Source.asInputStream(request)) {
            byte[] bytes = is.readAllBytes();
            if ("gzip".equalsIgnoreCase(request.getHeaders().get("Content-Encoding"))) {
                return CIPluginSDKUtils.inputStreamToUTF8String(new GZIPInputStream(new ByteArrayInputStream(bytes)));
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class RequestHandlingRule{
        public String urlPattern;
        public Predicate<Request> condition;
        public BiConsumer<Request, Response> operationOnRequest;
        public RequestHandlingRule(String urlPattern, Predicate<Request> cond, BiConsumer<Request, Response> op){
            this.urlPattern = urlPattern;
            this.condition = cond;
            this.operationOnRequest = op;
        }
    }

    public RestServerSimulator(int port){
        this.selectedPort = port;
    }


    public void startServer() {
        Handler.Sequence handlers = new Handler.Sequence();
        handlers.addHandler(this);
        server = new Server(selectedPort);
        server.setHandler(handlers);
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addRule(String urlPatter,  Predicate<Request> condition,
            BiConsumer<Request, Response> operationOnRequest) {

        handlingRules.add(new RequestHandlingRule(urlPatter, condition, operationOnRequest));
    }
    public List<Request> getReceivedRequests(){
        return receivedRequests;
    }
    public void endSimulation(){
        this.handlingRules.clear();
        this.receivedRequests.clear();
        try {
            this.server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {

        try {
            //  Jetty 12: getPathInContext() returns the path WITHOUT the query string. Several rule patterns
            //  (e.g. SSC ".../projects?q=name:...") rely on the query, so match against path + "?" + query.
            String pathInContext = Request.getPathInContext(request);
            String rawQuery = request.getHttpURI().getQuery();
            String matchTarget = rawQuery == null ? pathInContext : pathInContext + "?" + rawQuery;
            for (RequestHandlingRule handlingRule : handlingRules) {
                boolean urlMatch = true,
                        requestMatch = true;
                if (handlingRule.urlPattern != null &&
                        !Pattern.compile(handlingRule.urlPattern).matcher(matchTarget).matches()) {
                    urlMatch = false;
                }
                if (handlingRule.condition != null &&
                        !handlingRule.condition.test(request)) {
                    requestMatch = false;
                }
                if (urlMatch && requestMatch) {
                    CURRENT_CALLBACK.set(callback);
                    RESPONSE_WRITTEN.set(Boolean.FALSE);
                    try {
                        handlingRule.operationOnRequest.accept(request, response);
                    } finally {
                        boolean written = Boolean.TRUE.equals(RESPONSE_WRITTEN.get());
                        CURRENT_CALLBACK.remove();
                        RESPONSE_WRITTEN.remove();
                        //  if the handler wrote a body, the write already owns the callback; otherwise complete it here
                        if (!written) {
                            callback.succeeded();
                        }
                    }
                    return true;
                }
            }
            
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            callback.succeeded();
            return true;

        }finally {
            addRequestAsReceived(request);
        }
    }

    private void addRequestAsReceived(Request request) {
        receivedRequests.add(request);
    }
}
