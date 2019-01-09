package com.hp.octane.integrations.services.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import com.hp.octane.integrations.exceptions.PermanentException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.InputStream;

public class SonarUtils {

    public static InputStream getDataFromSonar(String projectKey , String token, URIBuilder uriQuery) {
        try {

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(uriQuery.build());
            setTokenInHttpRequest(request, token);

            HttpResponse httpResponse = httpClient.execute(request);
            return httpResponse.getEntity().getContent();

        } catch (Exception e) {
            String errorMessage = ""
                    .concat("failed to get data from sonar for project: ")
                    .concat(projectKey);
            throw new PermanentException(errorMessage, e);
        }
    }

    public static Boolean sonarReportHasAnotherPage(Integer pageIndex, JsonNode jsonContent) {
        JsonNode pagingNode = jsonContent.get("paging");
        Integer pageSize = pagingNode.get("pageSize").intValue();
        int total = pagingNode.get("total").intValue();
        return pageSize * pageIndex < total;
    }

    private static  void setTokenInHttpRequest(HttpRequest request, String token) throws AuthenticationException {
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(token, "");
        request.addHeader(new BasicScheme().authenticate(creds, request, null));
    }

}
