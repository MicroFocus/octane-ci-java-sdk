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

package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOBase;

/**
 * CI Server info DTO
 */

public interface CIServerInfo extends DTOBase {

    String getType();

    /**
     * Set CI server's type
     * The type is an unbound string, but please refer to an existing types in CIServerTypes enumeration for a matching existing one, if possible
     */
    CIServerInfo setType(String type);

    String getVersion();

    /**
     * Set CI server's version
     */
    CIServerInfo setVersion(String version);

    String getUrl();

    /**
     * Set CI server's self location URL
     */
    CIServerInfo setUrl(String url);

    String getInstanceId();

    /**
     * Set unique id of the CI server instance UUID
     */
    CIServerInfo setInstanceId(String instanceId);

    Long getInstanceIdFrom();

    /**
     * Set CI server's instance UUID creation time
     */
    CIServerInfo setInstanceIdFrom(Long instanceIdFrom);

    Long getSendingTime();

    /**
     * Set CI server's current time
     */
    CIServerInfo setSendingTime(Long sendingTime);

    String getImpersonatedUser();

    /**
     * Set CI server's user, that has been given to Octane's CI Plugin to run on behalf of
     */
    CIServerInfo setImpersonatedUser(String impersonatedUser);

    boolean isSuspended();

    /**
     * Set CI server's suspension status
     */
    CIServerInfo setSuspended(boolean suspended);

    String getSSCURL();

    CIServerInfo setSSCURL(String sscUrl);

    String getSSCBaseAuthToken();

    CIServerInfo setSSCBaseAuthToken(String sscBaseAuthToken);

    long getMaxPollingTimeoutHours();

    CIServerInfo setMaxPollingTimeoutHours(long maxPollingTimeoutHours);
}

