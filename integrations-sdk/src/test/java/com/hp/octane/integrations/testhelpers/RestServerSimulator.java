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

package com.hp.octane.integrations.testhelpers;

import org.apache.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class RestServerSimulator extends AbstractHandler {

    private int selectedPort;
    private Server server;
    private List<RequestHandlingRule> handlingRules = new ArrayList<>();
    private List<Request> receivedRequests = new ArrayList<>();

    public static class RequestHandlingRule{
        public String urlPattern;
        public Predicate<Request> condition;
        public Consumer<Request> operationOnRequest;
        public RequestHandlingRule(String urlPattern, Predicate<Request> cond, Consumer<Request> op){
            this.urlPattern = urlPattern;
            this.condition = cond;
            this.operationOnRequest = op;
        }
    }

    public RestServerSimulator(int port){
        this.selectedPort = port;
    }


    public void startServer() {
        HandlerCollection handlers = new HandlerCollection(true);
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
            Consumer<Request> operationOnRequest) {

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
    public void handle(String s, Request request,
                       HttpServletRequest httpServletRequest,
                       HttpServletResponse httpServletResponse) throws IOException, ServletException {

        try {
            for (RequestHandlingRule handlingRule : handlingRules) {
                boolean urlMatch = true,
                        requestMatch = true;
                if (handlingRule.urlPattern != null &&
                        !Pattern.compile(handlingRule.urlPattern).matcher(request.getOriginalURI()).matches()) {
                    urlMatch = false;
                }
                if (handlingRule.condition != null &&
                        !handlingRule.condition.test(request)) {
                    requestMatch = false;
                }
                if (urlMatch && requestMatch) {
                    handlingRule.operationOnRequest.accept(request);
                    request.setHandled(true);
                    break;
                }
            }
            if (!request.isHandled()) {
                request.setHandled(true);
                request.getResponse().setStatus(HttpStatus.SC_NOT_FOUND);
            }

        }finally {
            addRequestAsReceived(request);
        }
    }

    private void addRequestAsReceived(Request request) {
        receivedRequests.add(request);
    }
}
