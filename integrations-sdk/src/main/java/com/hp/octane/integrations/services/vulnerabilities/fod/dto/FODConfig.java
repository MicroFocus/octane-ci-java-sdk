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

/**
 * Created by hijaziy on 8/1/2017.
 */
public abstract class FODConfig {

    public final String authURL;
    public final String entitiesURL;
    public abstract String getAuthBody();

    protected FODConfig(String baseURL){

        String normanlizedURL = baseURL;
        if(!normanlizedURL.endsWith("/")){
            normanlizedURL = normanlizedURL + "/";
        }
        this.authURL = normanlizedURL + "oauth/token";
        this.entitiesURL = normanlizedURL + "api/v3";

    }


    public static class PasswordFODConfig extends FODConfig{

        String username;
        String password;
        String tenant;

        static final String authPWDBodyFormat ="grant_type=password&scope=https://hpfod.com/tenant&username=%s\\%s&password=%s";

        public PasswordFODConfig(String baseUrl, String username, String password,String tenant) {

            super(baseUrl);
            this.password = password;
            this.username = username;
            this.tenant = tenant;
        }
        @Override
        public String getAuthBody(){
            return String.format(authPWDBodyFormat,tenant,username,password);
        }
    }

    public static class CredentialsFODConfig extends FODConfig{

        static final String authBodyFormat ="grant_type=client_credentials&scope=https://hpfod.com/tenant&client_id=%s&client_secret=%s";
        String client_id;
        String secret;
        public CredentialsFODConfig(String baseUrl, String clientID, String secret){

            super(baseUrl);
            this.client_id = clientID;
            this.secret = secret;
        }

        @Override
        public String getAuthBody(){
            return String.format(authBodyFormat,client_id,secret);
        }
    }
}

