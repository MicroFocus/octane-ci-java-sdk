package com.hp.octane.integrations.services.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.hp.octane.integrations.api.SSCClient;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.vulnerabilities.SSCFortifyConfigurations;
import com.hp.octane.integrations.services.vulnerabilities.ssc.AuthToken;
import com.hp.octane.integrations.spi.CIPluginServices;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.hp.octane.integrations.services.rest.RestClientImpl.MAX_TOTAL_CONNECTIONS;

public class SSCClientImpl implements SSCClient {


    private final CloseableHttpClient httpClient;
    private AuthToken.AuthTokenData authTokenData;

    public SSCClientImpl() {


        SSLContext sslContext = SSLContexts.createSystemDefault();
        HostnameVerifier hostnameVerifier = new RestClientImpl.CustomHostnameVerifier();
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_TOTAL_CONNECTIONS);

        HttpClientBuilder clientBuilder = HttpClients.custom()
                .setConnectionManager(connectionManager);

        httpClient = clientBuilder.build();
    }

    public CloseableHttpResponse sendGetRequest(SSCFortifyConfigurations sscFortifyConfigurations, String url) {

        HttpGet request = new HttpGet(url);
        request.addHeader("Authorization", "FortifyToken " +
                getToken(sscFortifyConfigurations,false));
        request.addHeader("Accept", "application/json");
        request.addHeader("Host", getNetHost(sscFortifyConfigurations.serverURL));

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);
            //401. Access..
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED){
                request.removeHeaders("Authorization");
                request.addHeader("Authorization", "FortifyToken " +
                        getToken(sscFortifyConfigurations,true));
                response = httpClient.execute(request);
            }
            return response;
        } catch (IOException e) {
            throw new TemporaryException(e);
        } catch (Exception e) {
            throw new PermanentException(e);
        }
    }
    private String getToken(SSCFortifyConfigurations sscCfgs, boolean forceRenew) {
        if (forceRenew || authTokenData == null) {
            authTokenData = sendReqAuth(sscCfgs);
        }
        return this.authTokenData.token;
    }


    private AuthToken.AuthTokenData sendReqAuth(SSCFortifyConfigurations sscCfgs) {
        //"/{SSC Server Context}/api/v1"
        //String url = "http://" + serverURL + "/ssc/api/v1/projects?q=id:2743&fulltextsearch=true";
        String url = sscCfgs.serverURL + "/api/v1/tokens";
        HttpPost request = new HttpPost(url);
        request.addHeader("Authorization", sscCfgs.baseToken);
        request.addHeader("Accept", "application/json");
        request.addHeader("Host", getNetHost(sscCfgs.serverURL));
        request.addHeader("Content-Type", "application/json;charset=UTF-8");

        String body = "{\"type\": \"UnifiedLoginToken\"}";
        CloseableHttpResponse response = null;
        try {
            HttpEntity entity = new ByteArrayEntity(body.getBytes("UTF-8"));
            request.setEntity(entity);
            response = httpClient.execute(request);
            if (succeeded(response.getStatusLine().getStatusCode())) {

                String toString = isToString(response.getEntity().getContent());
                AuthToken authToken = new ObjectMapper().readValue(toString,
                        TypeFactory.defaultInstance().constructType(AuthToken.class));
                return authToken.data;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
                HttpClientUtils.closeQuietly(response);
            }
        }
        return null;
    }
    public static String getNetHost(String serverURL) {
        //http://myd-vma00564.swinfra.net:8180/ssc
        String prefix = "http://";
        int indexOfStart = serverURL.toLowerCase().indexOf(prefix) + prefix.length();
        int indexOfEnd = serverURL.lastIndexOf("/");
        if (indexOfEnd < 0) {
            return serverURL.substring(indexOfStart);
        }
        return serverURL.substring(indexOfStart, indexOfEnd);
    }

    public static boolean succeeded(int statusCode) {
        return statusCode == 200 || statusCode == 201;
    }
    public static String isToString(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString();
    }

}
