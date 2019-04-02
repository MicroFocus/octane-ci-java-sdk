package com.hp.octane.integrations.services.vulnerabilities.fod.dto;


import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.securityscans.FodServerConfiguration;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.Mock.FodMockSource;

/**
 * Created by hijaziy on 9/27/2017.
 */
public class FodConnectionFactory {

    private static FODSource fodSource;
    public final static Object syncObject = new Object();
    private static SecurityTool securityToolEntity;
    private static OctaneSDK.SDKServicesConfigurer configurer;

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

    private static SecurityTool getFODSecTool() {
        FodServerConfiguration fodProjectConfiguration =
                configurer.pluginServices.getFodServerConfiguration();
        return new SecurityTool(fodProjectConfiguration.getBaseUrl(),
                fodProjectConfiguration.getClientId(),
                fodProjectConfiguration.getClientSecret());
    }

    private static FODSource createFodConnector(SecurityTool securityToolEntity) {

        if(securityToolEntity.getToolUrl().contains("MockURL")){
            //TODO:
            return new FodMockSource();
        }else {
            FODConnector instance = new FODConnector(new FODConfig.CredentialsFODConfig(securityToolEntity.getToolUrl(),
                    securityToolEntity.getApiKey(),
                    securityToolEntity.getSecret()));
            instance.initConnection();
            return instance;
        }

    }

}
