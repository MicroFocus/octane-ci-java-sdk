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

package com.hp.octane.integrations.services.sonar;

import com.fasterxml.jackson.databind.JsonNode;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.InputStream;

public class SonarUtils {

    public static InputStream getDataFromSonar(String projectKey, String token, URIBuilder uriQuery) {
        StringBuilder errorMessage = new StringBuilder()
                .append("failed to get data from sonar for project key: ")
                .append(projectKey);

        try {

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(uriQuery.build());
            setTokenInHttpRequest(request, token);
            HttpResponse httpResponse = httpClient.execute(request);
            int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                return httpResponse.getEntity().getContent();
            } else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                errorMessage.append(" with status code: ").append(statusCode)
                        .append(" and response body: ").append(EntityUtils.toString(httpResponse.getEntity(), "UTF-8"));
                throw new TemporaryException(errorMessage.toString());
            } else {
                errorMessage.append(" with status code: ").append(statusCode)
                        .append(" and response body: ").append(EntityUtils.toString(httpResponse.getEntity(), "UTF-8"));
                throw new PermanentException(errorMessage.toString());
            }

        } catch (HttpHostConnectException e) {
             throw new TemporaryException(errorMessage.toString(), e);
        }
        catch (Exception e){
            throw new PermanentException(errorMessage.toString(),e);
        }
    }

    public static Boolean sonarReportHasAnotherPage(Integer pageIndex, JsonNode jsonContent) {
        JsonNode pagingNode = jsonContent.get("paging");
        Integer pageSize = pagingNode.get("pageSize").intValue();
        int total = pagingNode.get("total").intValue();
        return pageSize * pageIndex < total;
    }

    private static void setTokenInHttpRequest(HttpRequest request, String token) throws AuthenticationException {
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(token, "");
        request.addHeader(new BasicScheme().authenticate(creds, request, null));
    }

}
