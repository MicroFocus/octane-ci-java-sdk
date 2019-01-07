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

    static class RequestHandlingRule{
        RequestHandlingRule(String urlPattern, Predicate<Request> cond, Consumer<Request> op){
            this.urlPattern = urlPattern;
            this.condition = cond;
            this.operationOnRequest = op;
        }
        String urlPattern;
        Predicate<Request> condition;
        Consumer<Request> operationOnRequest;
    }

    public RestServerSimulator(int port){
        this.selectedPort = port;
    }

    int selectedPort;
    private Server server;
    private List<RequestHandlingRule> handlingRules = new ArrayList<>();
    List<Request> receivedRequests = new ArrayList<>();

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
                boolean urlMatch = true, requestMatch = true;
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
