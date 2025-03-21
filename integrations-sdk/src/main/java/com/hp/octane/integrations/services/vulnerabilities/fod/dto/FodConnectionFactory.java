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
package com.hp.octane.integrations.services.vulnerabilities.fod.dto;


import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.securityscans.FodServerConfiguration;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.mock.FodMockSource;

/**
 * Created by hijaziy on 9/27/2017.
 */
public class FodConnectionFactory {

    private static FODSource fodSource;
    public final static Object syncObject = new Object();
    private static SecurityTool securityToolEntity;
    private static OctaneSDK.SDKServicesConfigurer configurer;
    public final static String API_SUFFIX = "/api/v3";
    public static void setConfigurer(OctaneSDK.SDKServicesConfigurer configurer){
        FodConnectionFactory.configurer = configurer;
    }
    public static FODSource instance(){

        synchronized(syncObject) {
            if (needToUpdateConnection()) {
                fodSource = createFodConnector(securityToolEntity);
            }
            return fodSource;
        }
    }

    private static boolean needToUpdateConnection() {
        if(fodSource == null){
            securityToolEntity = getFODSecTool();
            return true;
        }
        SecurityTool updatedConnectionParams = getFODSecTool();
        if(!updatedConnectionParams.equals(securityToolEntity)){
            securityToolEntity = updatedConnectionParams;
            return true;
        }
        return false;
    }

    public static SecurityTool getFODSecTool() {
        FodServerConfiguration fodProjectConfiguration =
                configurer.pluginServices.getFodServerConfiguration();
        return new SecurityTool(fodProjectConfiguration.getApiUrl(),
                fodProjectConfiguration.getClientId(),
                fodProjectConfiguration.getClientSecret());
    }

    private static FODSource createFodConnector(SecurityTool securityToolEntity) {

        if(securityToolEntity.getToolUrlApi().contains("MockURL")){
            //TODO:
            return new FodMockSource();
        }else {
            FODConnector instance = new FODConnector(new FODConfig.CredentialsFODConfig(securityToolEntity.getToolUrlApi(),
                    securityToolEntity.getApiKey(),
                    securityToolEntity.getSecret()));
            instance.initConnection(configurer);
            return instance;
        }

    }

}
