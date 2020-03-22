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

package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOBase;

/**
 * CI Server info DTO
 */

public interface CIServerInfo extends DTOBase {

	/***
	 * Get type of CI server (jenkins,bamboo,goCd...)
	 * @return type of CI server
	 */
	String getType();

	/***
	 * 	 Set CI server's type
	 * 	 The type is an unbound string
	 * @param type CI server's type
	 * @return instance of current CIServerInfo
	 */
	CIServerInfo setType(String type);

	/***
	 * Get version of the CI server
	 * @return version of the CI server
	 */
	String getVersion();

	/***
	 * Set CI server's version
	 * @param version CI server's version
	 * @return instance of current CIServerInfo
	 */
	CIServerInfo setVersion(String version);

	/***
	 * Get self location URL to CI server
	 * @return self location URL to CI server
	 */
	String getUrl();

	/***
	 * Set CI server's self location URL
	 * @param url CI server's self location URL
	 * @return instance of current CIServerInfo
	 */
	CIServerInfo setUrl(String url);

	/***
	 * Get unique id of the CI server instance UUID
	 * @return unique id of the CI server
	 */
	String getInstanceId();

	/***
	 * Set unique id of the CI server instance UUID
	 * @param instanceId  unique id
	 * @return instance of current CIServerInfo
	 */
	CIServerInfo setInstanceId(String instanceId);

	/***
	 * Set CI server's instance creation time
	 * @return instance creation time
	 */
	Long getInstanceIdFrom();

	/***
	 * Set CI server's instance creation time
	 * @param instanceIdFrom CI server's instance creation time
	 * @return instance of current CIServerInfo
	 */
	CIServerInfo setInstanceIdFrom(Long instanceIdFrom);

	/***
	 * Get CI server's current time
	 * @return current time
	 */
	Long getSendingTime();

	/***
	 * Set CI server's current time
	 * @param sendingTime sendingTime
	 * @return instance of current CIServerInfo
	 */
	CIServerInfo setSendingTime(Long sendingTime);
}

