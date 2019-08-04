package com.hp.octane.integrations;

import com.hp.octane.integrations.exceptions.OctaneSDKGeneralException;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class OctaneConfiguration {
    private final String instanceId;
    private String url;
    private String sharedSpace;
    private String client;
    private String secret;
    volatile boolean attached;
    private String farm;


    private static final String PARAM_SHARED_SPACE = "p"; // NON-NLS
    private static final String UI_SPACE = "/ui";

    private static final String UNEXPECTED_SHARED_SPACE_EXCEPTION = "Unexpected shared space parameter value";
    private static final String APPLICATION_CONTEXT_NOT_FOUND_EXCEPTION = "Application context not found in URL";
    private static final String MISSING_SHARED_SPACE_EXCEPTION = "Missing shared space parameter";
    private static final String URL_INVALID_EXCEPTION = "Invalid URL";


    public OctaneConfiguration(String instanceId, String url, String sharedSpace) {
        if (instanceId == null || instanceId.isEmpty()) {
            throw new IllegalArgumentException("instance ID MUST NOT be null nor empty");
        }

        this.instanceId = instanceId;
        setUrl(url);
        setSharedSpace(sharedSpace);
    }

    public static OctaneConfiguration createWithUiLocation(String instanceId, String uiLocation) throws OctaneSDKGeneralException {
        try {

            //move all values after the #.
            String myUiLocation = uiLocation;
            int anchorPart = uiLocation.indexOf("#");
            if (anchorPart > 0) {
                myUiLocation = uiLocation.substring(0, anchorPart);
            }

            URL url = new URL(myUiLocation);
            String location;
            int contextPos = myUiLocation.indexOf(UI_SPACE);
            if (contextPos < 0) {
                throw new OctaneSDKGeneralException(APPLICATION_CONTEXT_NOT_FOUND_EXCEPTION);

            } else {
                location = myUiLocation.substring(0, contextPos);
            }
            List<NameValuePair> params = URLEncodedUtils.parse(url.toURI(), "UTF-8");
            for (NameValuePair param : params) {
                if (param.getName().equals(PARAM_SHARED_SPACE)) {
                    String[] sharedSpaceAndWorkspace = param.getValue().split("/");
                    // we are relaxed and allow parameter without workspace in order not to force user to makeup
                    // workspace value when configuring manually or via config API and not via copy & paste
                    if (sharedSpaceAndWorkspace.length < 1 || SdkStringUtils.isEmpty(sharedSpaceAndWorkspace[0])) {
                        throw new OctaneSDKGeneralException(UNEXPECTED_SHARED_SPACE_EXCEPTION);
                    }
                    return new OctaneConfiguration(instanceId, location, sharedSpaceAndWorkspace[0]);
                }
            }
            throw new OctaneSDKGeneralException(MISSING_SHARED_SPACE_EXCEPTION);
        } catch (MalformedURLException e) {
            throw new OctaneSDKGeneralException(URL_INVALID_EXCEPTION);
        } catch (URISyntaxException e) {
            throw new OctaneSDKGeneralException(URL_INVALID_EXCEPTION);
        }
    }

    public final String getInstanceId() {
        return instanceId;
    }

    public final String getUrl() {
        return url;
    }

    public final void setUrl(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("url MUST NOT be null nor empty");
        }

        try {
            URL tmp = new URL(url);
            String tmpFarm = tmp.getHost() + (tmp.getPort() > 0 ? (":" + tmp.getPort()) : "");
            if ((tmp.getProtocol() + "://" + tmpFarm).equals(this.url)) {
                return;
            }

            if (attached && !OctaneSDK.isSharedSpaceUnique(tmpFarm, sharedSpace)) {
                throw new IllegalArgumentException("shared space '" + sharedSpace + "' of Octane '" + tmpFarm + "' is already in use");
            }

            farm = tmpFarm;
            this.url = tmp.getProtocol() + "://" + tmpFarm;
        } catch (MalformedURLException mue) {
            throw new IllegalArgumentException(URL_INVALID_EXCEPTION, mue);
        }
    }

    public final String getSharedSpace() {
        return sharedSpace;
    }

    public final void setSharedSpace(String sharedSpace) {
        if (sharedSpace == null || sharedSpace.isEmpty()) {
            throw new IllegalArgumentException("shared space ID MUST NOT be null nor empty");
        }

        if (sharedSpace.equals(this.sharedSpace)) {
            return;
        }

        if (attached && !OctaneSDK.isSharedSpaceUnique(farm, sharedSpace)) {
            throw new IllegalArgumentException("shared space '" + sharedSpace + "' of Octane '" + farm + "' is already in use");
        }

        this.sharedSpace = sharedSpace;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        if (this.client != null && !SdkStringUtils.equals(this.client, client)) {
            try {
                ((OctaneClientImpl) OctaneSDK.getClientByInstanceId(this.instanceId)).notifyCredentialsChanged();
            } catch (IllegalArgumentException e) {
                //failed to get client by instance id, possibly client not valid and no need to notify ,
                // therefore do nothing
            }
        }
        this.client = client;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    final String getFarm() {
        return farm;
    }

    @Override
    public String toString() {
        return "OctaneConfiguration { " +
                "instanceId: " + instanceId +
                ", url: " + url +
                ", sharedSpace: " + sharedSpace +
                ", client: " + client + " }";
    }
}
