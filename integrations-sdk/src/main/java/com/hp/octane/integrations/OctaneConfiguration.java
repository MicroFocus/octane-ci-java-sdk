package com.hp.octane.integrations;

import com.hp.octane.integrations.exceptions.OctaneSDKGeneralException;
import com.hp.octane.integrations.utils.OctaneUrlParser;
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

    public OctaneConfiguration(String instanceId, String url, String sharedSpace) {
        if (instanceId == null || instanceId.isEmpty()) {
            throw new IllegalArgumentException("instance ID MUST NOT be null nor empty");
        }

        this.instanceId = instanceId;
        setUrl(url);
        setSharedSpace(sharedSpace);
    }

    public static OctaneConfiguration createWithUiLocation(String instanceId, String uiLocation) throws OctaneSDKGeneralException {

        OctaneUrlParser parseLocation = OctaneUrlParser.parse(uiLocation);
        return new OctaneConfiguration(instanceId, parseLocation.getLocation(), parseLocation.getSharedSpace());
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
            throw new IllegalArgumentException("Invalid URL", mue);
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
