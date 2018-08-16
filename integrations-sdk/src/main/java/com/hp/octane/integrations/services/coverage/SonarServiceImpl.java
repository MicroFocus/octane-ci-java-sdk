package com.hp.octane.integrations.services.coverage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.api.RestService;
import com.hp.octane.integrations.api.SonarService;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.coverage.BuildCoverage;
import com.hp.octane.integrations.exceptions.OctaneSDKSonarException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.hp.octane.integrations.api.RestService.*;


public class SonarServiceImpl extends OctaneSDK.SDKServiceBase implements SonarService {


    private static String WEBHOOK_CREATE_URI = "/api/webhooks/create";
    private static String WEBHOOK_DELETE_URI = "/api/webhooks/delete";
    private static String WEBHOOK_LIST_URI = "/api/webhooks/list";
    private static String SONAR_STATUS_URI = "/api/system/status";
    private static String CONNECTION_FAILURE = "CONNECTION_FAILURE";

    public static String COMPONENT_TREE_URI = "/api/measures/component_tree";
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();
    private RestService restService;
    private static final Logger logger = LogManager.getLogger(SonarServiceImpl.class);


    Map<String, SonarAuthentication> sonarAuthenticationMap = new HashMap<>();
    static ObjectMapper objectMapper = new ObjectMapper();


    public SonarServiceImpl(Object internalUsageValidator, RestService restService) {
        super(internalUsageValidator);
        this.restService = restService;
    }

    @Override
    public void setSonarAuthentication(String projectKey, String sonarURL, String token) {
        sonarAuthenticationMap.put(projectKey, new SonarAuthentication(sonarURL, token));
    }


    @Override
    public String registerWebhook(String ciNotificationUrl, String projectKey, String jenkinsJob) throws OctaneSDKSonarException {

        String sonarURL = sonarAuthenticationMap.get(projectKey).getUrl();
        String token = sonarAuthenticationMap.get(projectKey).getToken();

        try {
            String webhookKey = getWebhookKey(projectKey, ciNotificationUrl);
            if (webhookKey != null) { //webhook with the same parameters already exists
                return webhookKey;
            } else {//create new webhook

                HttpClient httpClient = HttpClientBuilder.create().build();

                URIBuilder uriBuilder = new URIBuilder(sonarURL + WEBHOOK_CREATE_URI)
                        .setParameter("name", projectKey)
                        .setParameter("url", ciNotificationUrl);

                HttpPost request = new HttpPost(uriBuilder.toString());
                setTokenInHttpRequest(request, token);
                HttpResponse response = httpClient.execute(request);

                JsonNode jsonResponse = objectMapper.readTree(response.getEntity().getContent());
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    String errorMessage = new StringBuilder()
                            .append("exception during webhook registration for project key: ").append(projectKey)
                            .append(" with ciNotificationUrl: ").append(ciNotificationUrl)
                            .append(" with status code: ").append(response.getStatusLine().getStatusCode())
                            .append(" with errors: ").append(jsonResponse.get("errors").toString()).toString();
                    throw new OctaneSDKSonarException(errorMessage);
                }
                return jsonResponse.get("webhook").get("key").textValue();
            }

        } catch (OctaneSDKSonarException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            String errorMessage = new StringBuilder()
                    .append("exception during webhook registration for project key: ").append(projectKey)
                    .append(" with ciNotificationUrl: ").append(ciNotificationUrl)
                    .toString();

            logger.error(errorMessage, e);
            throw new OctaneSDKSonarException(errorMessage, e);
        }
    }


    @Override
    public void unregisterWebhook(String projectKey, String jenkinsJob) throws OctaneSDKSonarException {
        String sonarURL = sonarAuthenticationMap.get(projectKey).getUrl();
        String token = sonarAuthenticationMap.get(projectKey).getToken();

        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            // TODO: FIX
            URIBuilder uriBuilder = new URIBuilder(sonarURL + WEBHOOK_DELETE_URI)
                    .setParameter("webhook", "FIX");

            HttpPost request = new HttpPost(uriBuilder.toString());
            setTokenInHttpRequest(request, token);
            httpClient.execute(request);

            HttpResponse response = httpClient.execute(request);
            JsonNode jsonResponse = objectMapper.readTree(response.getEntity().getContent());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                String errorMessage = new StringBuilder()
                        .append("exception during webhook unregistration for project key: ").append(projectKey)
                        .append(" with status code: ").append(response.getStatusLine().getStatusCode())
                        .append(" with errors: ").append(jsonResponse.get("errors").toString()).toString();
                throw new OctaneSDKSonarException(errorMessage);
            }

        } catch (OctaneSDKSonarException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            String errorMessage = new StringBuilder()
                    .append("exception during webhook unregistration for project key: ").append(projectKey).toString();

            logger.error(errorMessage, e);
            throw new OctaneSDKSonarException(errorMessage, e);
        }

    }


    @Override
    public String getSonarStatus(String projectKey) {

        String sonarUrl = sonarAuthenticationMap.get(projectKey).getUrl();
        try {
            URIBuilder uriBuilder = new URIBuilder(sonarUrl + SONAR_STATUS_URI);

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(uriBuilder.build());
            HttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return objectMapper.readTree(response.getEntity().getContent()).get("status").textValue();
            } else {
                return CONNECTION_FAILURE;
            }
        } catch (Exception e) {
            return CONNECTION_FAILURE;
        }
    }


    @Override
    public void injectSonarDataToOctane(String projectKey, String ciIdentity, String jobId, String buildId) throws OctaneSDKSonarException {


        StringBuilder errorMessage = new StringBuilder()
                .append("failed to inject sonarqube coverage data to octane for project key: ")
                .append(projectKey)
                .append(" with ciIdentity: ").append(ciIdentity)
                .append(" with jobId: ").append(jobId)
                .append(" with buildId: ").append(buildId);

        try {

            Integer pageIndex = 0;

            BuildCoverage buildCoverageReport = dtoFactory.newDTO(BuildCoverage.class);

            InputStream reportStream;
            do {
                pageIndex++;
                reportStream = getPageFromSonar(projectKey, pageIndex);
                buildCoverageReport.mergeSonarCoverageReport(reportStream);
            } while (coverageReportHasAnotherPage(pageIndex, reportStream));

            OctaneRequest coveragePutRequest = buildCoveragePutRequest(buildCoverageReport, ciIdentity, jobId, buildId);
            OctaneResponse response = restService.obtainClient().execute(coveragePutRequest);

            if (response.getStatus() != HttpStatus.SC_OK) {
                errorMessage.append(" with status code: ").append(response.getStatus())
                        .append(" and response body: ").append(response.getBody());
                throw new OctaneSDKSonarException(errorMessage.toString());
            }

        } catch (OctaneSDKSonarException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error(errorMessage.toString(), e);
            throw new OctaneSDKSonarException(errorMessage.toString(), e);
        }
    }

    private Boolean coverageReportHasAnotherPage(Integer pageIndex, InputStream content) throws IOException {
        JsonNode jsonContent = objectMapper.readTree(content);

        JsonNode pagingNode = jsonContent.get("paging");
        Integer pageSize = pagingNode.get("pageSize").intValue();
        Integer total = pagingNode.get("total").intValue();
        return pageSize * pageIndex < total;
    }

    private OctaneRequest buildCoveragePutRequest(BuildCoverage buildCoverage, String ciIdentity, String jobId, String buildId) throws URISyntaxException, JsonProcessingException {

        URIBuilder uriBuilder = new URIBuilder(getAnalyticsContextPath(pluginServices.getOctaneConfiguration().getUrl(), pluginServices.getOctaneConfiguration().getSharedSpace()) + "coverage")
                .setParameter("ci-server-identity", ciIdentity)
                .setParameter("ci-job-id", jobId)
                .setParameter("ci-build-id", buildId)
                .setParameter("file-type", SonarService.SONAR_TYPE);

        String reportToOctane = objectMapper.writeValueAsString(buildCoverage);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());
        OctaneRequest coverageRequest = dtoFactory.newDTO(OctaneRequest.class)
                .setMethod(HttpMethod.PUT)
                .setUrl(uriBuilder.toString())
                .setHeaders(headers)
                .setBody(reportToOctane);
        return coverageRequest;
    }


    public String getWebhookKey(String projectKey, String url) throws OctaneSDKSonarException {

        String sonarURL = sonarAuthenticationMap.get(projectKey).getUrl();
        String token = sonarAuthenticationMap.get(projectKey).getToken();


        try {
            URIBuilder uriBuilder = new URIBuilder(sonarURL + WEBHOOK_LIST_URI);
            uriBuilder.setParameter("project", "hpe:demo");

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(uriBuilder.build());
            setTokenInHttpRequest(request, token);

            HttpResponse response = httpClient.execute(request);
            JsonNode jsonResponse = objectMapper.readTree(response.getEntity().getContent());
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                ArrayNode webhooksListJson = (ArrayNode) jsonResponse.get("webhooks");
                if (webhooksListJson.size() > 0) {
                    for (JsonNode webhookNode : webhooksListJson) {
                        String entryName = webhookNode.get("name").textValue();
                        String entryURL = webhookNode.get("url").textValue();
                        if (entryName.equals(projectKey) && entryURL.equals(url)) {
                            return webhookNode.get("key").textValue();
                        }
                    }
                }
                return null;

            } else {
                String errorMessage = new StringBuilder()
                        .append("failed to get webhook key from soanrqube for project key: ").append(projectKey)
                        .append(" with url: ").append(url)
                        .append(" with status code: ").append(response.getStatusLine().getStatusCode())
                        .append(" with errors: ").append(jsonResponse.get("errors").toString()).toString();
                throw new OctaneSDKSonarException(errorMessage);

            }
        } catch (OctaneSDKSonarException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            String errorMessage = new StringBuilder()
                    .append("failed to get webhook key from soanrqube for project key: ").append(projectKey)
                    .append(" with url: ").append(url).toString();
            logger.error(errorMessage, e);
            throw new OctaneSDKSonarException(errorMessage, e);
        }
    }


    private InputStream getPageFromSonar(String projectKey, Integer page) throws OctaneSDKSonarException {
        try {
            String sonarURL = sonarAuthenticationMap.get(projectKey).getUrl();
            String token = sonarAuthenticationMap.get(projectKey).getToken();

            URIBuilder uriBuilder = new URIBuilder(sonarURL + COMPONENT_TREE_URI);
            uriBuilder.setParameter("metricKeys", "lines_to_cover,uncovered_lines")
                    .setParameter("component", projectKey)
                    .setParameter("qualifiers", "FIL,TRK")
                    .setParameter("ps", "500")
                    .setParameter("p", page.toString());

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(uriBuilder.build());
            setTokenInHttpRequest(request, token);

            HttpResponse httpResponse = httpClient.execute(request);
            return httpResponse.getEntity().getContent();

        } catch (Exception e) {

            String errorMessage = new StringBuilder()
                    .append("failed to get coverage data from sonar for project: ")
                    .append(projectKey).toString();
            logger.error(errorMessage, e);
            throw new OctaneSDKSonarException(errorMessage, e);
        }
    }

    private void setTokenInHttpRequest(HttpRequest request, String token) throws AuthenticationException {
        UsernamePasswordCredentials creds
                = new UsernamePasswordCredentials(token, "");
        request.addHeader(new BasicScheme().authenticate(creds, request, null));
    }

    private static class SonarAuthentication {
        private String url;
        private String token;

        public SonarAuthentication(String url, String token) {
            this.url = url;
            this.token = token;
        }

        public String getUrl() {
            return url;
        }

        public String getToken() {
            return token;
        }
    }

    private String getAnalyticsContextPath(String octaneBaseUrl, String sharedSpaceId) {
        return octaneBaseUrl + SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + ANALYTICS_CI_PATH_PART;
    }
}
