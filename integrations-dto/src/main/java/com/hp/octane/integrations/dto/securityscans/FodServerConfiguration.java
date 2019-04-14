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
 *
 */
package com.hp.octane.integrations.dto.securityscans;

import com.hp.octane.integrations.dto.DTOBase;

public interface FodServerConfiguration extends DTOBase {

    String getApiUrl();
    FodServerConfiguration setApiUrl(String sscUrl);

    String getClientId();
    FodServerConfiguration setClientId(String sscBaseAuthToken);

    String getClientSecret();
    FodServerConfiguration setClientSecret(String sscBaseAuthToken);

    long getMaxPollingTimeoutHours();
    FodServerConfiguration setMaxPollingTimeoutHours(long maxPollingTimeoutHours);

    boolean isValid();

    FodServerConfiguration setBaseUrl(String baseUrl);
    String getBaseUrl();
}
