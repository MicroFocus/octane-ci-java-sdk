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
package com.hp.octane.integrations.services.vulnerabilities.fod.dto;


public class SecurityTool {

    private String toolUrl;
    private String apiKey;
    private String secret;

    public SecurityTool(String toolUrl, String apiKey, String secret) {
        this.toolUrl = toolUrl;
        this.apiKey = apiKey;
        this.secret = secret;
    }

    public String getToolUrl() {
        return toolUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getSecret() {
        return secret;
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof SecurityTool)){
            return false;
        }

        SecurityTool securityTool = (SecurityTool) obj;
        return genericEquals(apiKey, securityTool.apiKey) &&
                genericEquals(secret, securityTool.secret) &&
                genericEquals(toolUrl, securityTool.toolUrl);
    }
    @Override
    public int hashCode(){
        return super.hashCode();
    }

    static boolean genericEquals(Object obj1, Object obj2){
        if(obj1 == null && obj2 == null){
            return true;
        }
        if(obj1 == null){
            return false;
        }
        if(obj2 == null){
            return false;
        }
        return obj1.equals(obj2);
    }

}
