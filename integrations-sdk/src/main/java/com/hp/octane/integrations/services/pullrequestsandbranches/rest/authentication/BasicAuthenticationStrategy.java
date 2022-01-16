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

package com.hp.octane.integrations.services.pullrequestsandbranches.rest.authentication;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.RequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicAuthenticationStrategy extends AuthenticationStrategy {
    private final String userName;
    private final String password;

    public BasicAuthenticationStrategy(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    @Override
    public void onCreateHttpRequest(RequestBuilder requestBuilder) {
        String encoding = Base64.getEncoder().encodeToString((userName + ":" + password).getBytes(StandardCharsets.UTF_8));
        requestBuilder.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
    }

    public String getUserName() {
        return userName;
    }
}
