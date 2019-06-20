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
package com.hp.octane.integrations.services.vulnerabilities.fod.dto.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.FODEntityCollection;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FODUser {
    @JsonProperty(value = "userId")
    public int userId;
    @JsonProperty(value = "userName")
    public String userName;
    @JsonProperty(value = "firstName")
    public String firstName;
    @JsonProperty(value = "lastName")
    public String lastName;
    @JsonProperty(value = "email")
    public String email;

    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class Users extends FODEntityCollection<FODUser> {

    }
}
