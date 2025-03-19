/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
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
package com.hp.octane.integrations;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;
import com.hp.octane.integrations.utils.OctaneUrlParser;
import com.hp.octane.integrations.utils.SdkStringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OctaneConfiguration {
    private final String instanceId;
    private String url;
    private String sharedSpace;
    private String client;
    private String secret;
    volatile boolean attached;
    private boolean suspended;
    private boolean sdkSupported = true;
    private String impersonatedUser;
    private Map<String, ConfigurationParameter> parameters = new HashMap<>();

    public OctaneConfiguration(String instanceId) {
        if (instanceId == null || instanceId.isEmpty()) {
            throw new IllegalArgumentException("instance ID MUST NOT be null nor empty");
        }
        this.instanceId = instanceId;
    }

    public static OctaneConfiguration createWithUiLocation(String instanceId, String uiLocation) throws IllegalArgumentException {
        OctaneConfiguration oc = new OctaneConfiguration(instanceId);
        oc.setUiLocation(uiLocation);
        return oc;
    }

    public static OctaneConfiguration create(String instanceId, String serverUrl, String sharedSpace) throws IllegalArgumentException {
        OctaneConfiguration oc = new OctaneConfiguration(instanceId);
        oc.setUrlAndSpace(serverUrl, sharedSpace);
        return oc;
    }

    public final String getInstanceId() {
        return instanceId;
    }

    public final String getUrl() {
        return url;
    }

    public final void setUiLocation(String uiLocation) {
        OctaneUrlParser parseLocation = OctaneUrlParser.parse(uiLocation);
        setUrlAndSpace(parseLocation.getLocation(), parseLocation.getSharedSpace());
    }

    public final void setUrlAndSpace(String serverUrl, String space) {

        if (serverUrl == null || serverUrl.trim().isEmpty()) {
            throw new IllegalArgumentException(OctaneUrlParser.URL_INVALID_EXCEPTION);
        }
        if (space == null || space.trim().isEmpty()) {
            throw new IllegalArgumentException(OctaneUrlParser.MISSING_SHARED_SPACE_EXCEPTION);
        }
        String myServerUrl = serverUrl.trim();
        String mySpace = space.trim();
        if (myServerUrl.contains("?")) {
            myServerUrl = myServerUrl.substring(0, myServerUrl.indexOf("?"));
        }

        try {
            new URL(myServerUrl);//validation of url validity
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(OctaneUrlParser.URL_INVALID_EXCEPTION);
        }

        if (myServerUrl.equals(this.url) && mySpace.equals(this.sharedSpace)) {
            return;
        }

        if (attached && !OctaneSDK.isSharedSpaceUnique(myServerUrl, mySpace)) {
            throw new IllegalArgumentException("shared space '" + mySpace + "' of Octane '" + myServerUrl + "' is already in use");
        }

        this.url = myServerUrl;
        this.sharedSpace = mySpace;
    }

    public final String getSharedSpace() {
        return sharedSpace;
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

    @Override
    public String toString() {
        return "OctaneConfiguration { " +
                "instanceId: " + instanceId +
                ", url: " + url +
                ", sharedSpace: " + sharedSpace +
                ", suspended: " + suspended +
                ", client: " + client + " }";
    }

    public String getLocationForLog() {
        return "[" + getUrl() + "?p=" + getSharedSpace() + "] ";
    }

    public boolean isSuspended() {
        return suspended;
    }

    public boolean isDisabled() {
        return suspended || !sdkSupported;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public String getImpersonatedUser() {
        return impersonatedUser;
    }

    public void setImpersonatedUser(String impersonatedUser) {
        this.impersonatedUser = impersonatedUser;
    }

    public boolean isSdkSupported() {
        return sdkSupported;
    }

    protected void setSdkSupported(boolean sdkSupported) {
        this.sdkSupported = sdkSupported;
    }

    public void clearParameters() {
        this.parameters.clear();
    }

    public Set<String> getParameterNames() {
        return this.parameters.keySet();
    }

    public void addParameter(ConfigurationParameter param) {
        this.parameters.put(param.getKey(), param);
    }

    public ConfigurationParameter getParameter(String key) {
        return this.parameters.get(key);
    }

    public boolean isParameterDefined(String key) {
        return this.parameters.containsKey(key);
    }
}
