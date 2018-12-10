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

package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.services.rest.SSCRestClient;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by hijaziy on 7/12/2018.
 */
public class SscProjectConnector {
    private final SSCProjectConfiguration sscProjectConfiguration;
    private final SSCRestClient sscRestClient;
    private final static Logger logger = LogManager.getLogger(SscProjectConnector.class);

    public SscProjectConnector(SSCProjectConfiguration sscProjectConfiguration, SSCRestClient sscRestClient) {
        this.sscProjectConfiguration = sscProjectConfiguration;
        this.sscRestClient = sscRestClient;
    }

    private String sendGetEntity(String urlSuffix) {
        String url = sscProjectConfiguration.getSSCUrl() + "/api/v1/" + urlSuffix;
        CloseableHttpResponse response = sscRestClient.sendGetRequest(sscProjectConfiguration, url);

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
            throw new TemporaryException("SSC Server is not available:" + response.getStatusLine().getStatusCode());
        } else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new PermanentException("Error from SSC:" + response.getStatusLine().getStatusCode());
        }
        try {
            return CIPluginSDKUtils.inputStreamToUTF8String(response.getEntity().getContent());
        } catch (IOException e) {
            throw new PermanentException(e);
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
            HttpClientUtils.closeQuietly(response);
        }
    }

    public ProjectVersions.ProjectVersion getProjectVersion() {
        Integer projectId = getProjectId();
        if (projectId == null) {
            return null;
        }
        String suffix = getURLForProjectVersion(projectId);
        String rawResponse = sendGetEntity(suffix);
        ProjectVersions projectVersions = stringToObject(rawResponse, ProjectVersions.class);
        if (projectVersions.getCount() == 0) {
            return null;
        }
        return projectVersions.getData().get(0);
    }

    private Integer getProjectId() {
        String projectIdURL = getProjectIdURL();
        String rawResponse = sendGetEntity(projectIdURL);
        Projects projects = stringToObject(rawResponse, Projects.class);
        if (projects.getCount() == 0) {
            return null;
        }
        return projects.getData().get(0).id;
    }

    public static <T> T stringToObject(String response, Class<T> type) {
        if (response == null) {
            return null;
        }
        try {
            return new ObjectMapper().readValue(response,
                    TypeFactory.defaultInstance().constructType(type));
        } catch (IOException e) {
            throw new PermanentException(e);
        }
    }

    public Issues readNewIssuesOfLatestScan(int projectVersionId) {
        String urlSuffix = getNewIssuesURL(projectVersionId);
        return readPagedEntities(urlSuffix, Issues.class);
    }

    public Issues readIssues(int projectVersionId, String state) {
        String urlSuffix = getIssuesURL(projectVersionId, state);
        return readPagedEntities(urlSuffix, Issues.class);
    }

    public <SSCArray extends SscBaseEntityArray> SSCArray readPagedEntities(String url, Class<SSCArray> type) {
        int startIndex = 0;

        try {
            boolean allFetched = false;
            SSCArray total = type.newInstance();
            while (!allFetched) {
                String pagedURL = getPagedURL(url, startIndex);
                String rawResponse = sendGetEntity(pagedURL);
                SSCArray page = stringToObject(rawResponse, type);
                if (total.getData() == null) {
                    total.setData(page.getData());
                } else {
                    total.getData().addAll(page.getData());
                }
                total.setCount(total.getData().size());
                allFetched = (total.getData().size() == page.getCount());
                startIndex += total.getCount();
            }
            return total;
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    private String getPagedURL(String url, int startIndex) {

        if (url.contains("?")) {
            return url + "&start=" + startIndex;
        } else {
            return url + "?start=" + startIndex;
        }
    }


    public Artifacts getArtifactsOfProjectVersion(Integer id, int limit) {
        String urlSuffix = getArtifactsURL(id, limit);
        String rawResponse = sendGetEntity(urlSuffix);
        return stringToObject(rawResponse, Artifacts.class);
    }

    public String getProjectIdURL() {
        return "projects?q=name:" + CIPluginSDKUtils.urlEncodePathParam(this.sscProjectConfiguration.getProjectName());
    }

    public String getNewIssuesURL(int projectVersionId) {
        return String.format("projectVersions/%d/issues?showhidden=false&showremoved=false&showsuppressed=false", projectVersionId);
    }

    public String getIssuesURL(int projectVersionId, String state) {
        if ("updated".equalsIgnoreCase(state)) {
            return String.format("projectVersions/%d/issues?q=[issue_age]:!new&qm=issues&showhidden=false&showremoved=false&showsuppressed=false",
                    projectVersionId);
        }
        return null;
    }

    public String getRemoteTag() {
       return sscProjectConfiguration.getRemoteTag();
    }

    public String getArtifactsURL(Integer projectVersionId, int limit) {
        return String.format("projectVersions/%d/artifacts?limit=%d", projectVersionId, limit);
    }

    public String getURLForProjectVersion(Integer projectId) {
        return "projects/" + projectId + "/versions?q=name:" + CIPluginSDKUtils.urlEncodePathParam(this.sscProjectConfiguration.getProjectVersion());
    }
}
