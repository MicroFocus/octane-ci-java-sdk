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

package com.hp.octane.integrations.testhelpers;

import com.hp.octane.integrations.services.vulnerabilities.SSCInput;
import com.hp.octane.integrations.services.vulnerabilities.SSCTestUtils;
import com.hp.octane.integrations.services.vulnerabilities.ssc.AuthToken;
import com.hp.octane.integrations.services.vulnerabilities.ssc.IssueDetails;
import com.hp.octane.integrations.services.vulnerabilities.ssc.ProjectVersions;
import com.hp.octane.integrations.services.vulnerabilities.ssc.Projects;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.server.Request;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

public class SSCServerSimulator extends RestServerSimulator{

    static int selectedPort = 9090;
    static SSCServerSimulator _instance;

    public SSCServerSimulator(int port) {
        super(port);
    }

    public static void setSelectedPort(int port){
        selectedPort = port;
    }
    public static String getSimulatorUrl() {
        return "http://localhost:"+selectedPort;
    }


    public static synchronized SSCServerSimulator instance(){
        if(_instance == null){
            _instance = new SSCServerSimulator(selectedPort);
        }
        return _instance;
    }

    public void setDefaultAuth() {
        setAuthHanler(request -> {
            try {
                AuthToken.AuthTokenData authTokenData = new AuthToken.AuthTokenData();
                authTokenData.token = "DUMMY TOKEN";
                authTokenData.terminalDate = "TERMINAL_DATE";
                AuthToken authToken = new AuthToken();
                authToken.setData(authTokenData);

                request.getResponse().setStatus(HttpStatus.SC_OK);
                request.getResponse().getWriter().write(SSCTestUtils.getJson(authToken));
                request.getResponse().getWriter().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    public void setAuthHanler(Consumer<Request> authHandler) {
        SSCServerSimulator.instance().addRule("^.*/api/v1/tokens.*",
                t->t.getMethod().equalsIgnoreCase("post"),
                authHandler);
    }
    public void setSequenceToSimulator(SSCInput sscInput) {
        setProject(sscInput);
        setProjectVersion(sscInput);
        setArtifacts(sscInput);
        setIssues(sscInput);
        setIssueDetails();
    }

    private void setIssueDetails() {

        addRule("^.*/api/v1/issueDetails/.*",
                t -> t.getMethod().equalsIgnoreCase("get"),
                request -> {
                    try {
                        request.getResponse().setStatus(HttpStatus.SC_OK);
                        String originalURI = request.getOriginalURI();
                        String issueId = originalURI.substring(originalURI.lastIndexOf("/"));
                        IssueDetails issueDetails = new IssueDetails();
                        IssueDetails.IssueDetailsData issueDetailsData = new IssueDetails.IssueDetailsData();
                        issueDetailsData.tips = "tips:" + issueId;
                        issueDetailsData.recommendation = "recommendation:" + issueId;
                        issueDetailsData.detail = "details:" + issueId;
                        issueDetailsData.brief = "brief:" + issueId;
                        issueDetails.setData(issueDetailsData);
                        String json = SSCTestUtils.getJson(issueDetails);
                        request.getResponse().getWriter().write(json);
                        request.getResponse().getWriter().flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public void setIssues(SSCInput sequence) {

        addRule("^.*/api/v1/projectVersions/" + sequence.projectVersionId + "/issues.*",
                t -> t.getMethod().equalsIgnoreCase("get"),
                request -> {
                    try {
                        request.getResponse().setStatus(HttpStatus.SC_OK);
                        String json = SSCTestUtils.getJson(sequence.getIssuesToReturn());
                        request.getResponse().getWriter().write(json);
                        request.getResponse().getWriter().flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public void setArtifacts(SSCInput sequence) {

       addRule("^.*/api/v1/projectVersions/" + sequence.projectVersionId + "/artifacts.*",
                t -> t.getMethod().equalsIgnoreCase("get"),
                request -> {
                    try {
                        request.getResponse().setStatus(HttpStatus.SC_OK);
                        String json = SSCTestUtils.getJson(sequence.artifacts);
                        request.getResponse().getWriter().write(json);
                        request.getResponse().getWriter().flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

    }

    public void setProjectVersion(SSCInput sequence) {
        instance().addRule("^.*/api/v1/projects/"+sequence.projectId+"/versions\\?q=name:.*",
                t->t.getMethod().equalsIgnoreCase("get") &&
                        queryIsAboutProjectName("name",sequence.projectVersionName,t),
                request-> {
                    try {
                        ProjectVersions projectVersions = new ProjectVersions();
                        projectVersions.setCount(1);
                        ProjectVersions.ProjectVersion projectVersion = new ProjectVersions.ProjectVersion();
                        projectVersion.id = sequence.projectVersionId;
                        projectVersions.setData(Arrays.asList(projectVersion));
                        request.getResponse().setStatus(HttpStatus.SC_OK);
                        request.getResponse().getWriter().write(SSCTestUtils.getJson(projectVersions));
                        request.getResponse().getWriter().flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
    public boolean queryIsAboutProjectName(String paramName, String paramValue, Request request) {
        request.mergeQueryParameters("", request.getQueryString(), false);
        String queryString = request.getQueryParameters().getString("q");
        return queryString != null && queryString.substring((paramName + ":").length()).startsWith(paramValue);
    }
    public void setProject(SSCInput sequence) {

        addRule("^.*/api/v1/projects\\?q=name:.*",
                t->t.getMethod().equalsIgnoreCase("get") &&
                        queryIsAboutProjectName("name",sequence.projectName, t),
                request-> {
                    try {
                        Projects projects = new Projects();
                        projects.setCount(1);
                        Projects.Project project = new Projects.Project();
                        project.id = sequence.projectId;
                        project.name = sequence.projectName;
                        projects.setData(Arrays.asList(project));
                        request.getResponse().setStatus(HttpStatus.SC_OK);
                        request.getResponse().getWriter().write(SSCTestUtils.getJson(projects));
                        request.getResponse().getWriter().flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}
