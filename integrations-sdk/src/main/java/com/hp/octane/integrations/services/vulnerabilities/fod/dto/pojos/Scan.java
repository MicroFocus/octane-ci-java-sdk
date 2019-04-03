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

import java.io.Serializable;


/**
 * Created by hijaziy on 7/30/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Scan  implements Serializable{

    public final static String IN_PROGRESS = "In_Progress";
    public final static String COMPLETED = "Completed";
    public final static String NOT_STARTED = "Not_Started";

    @JsonProperty("scanId")
    public Long scanId;

    @JsonProperty("releaseId")
    public Long releaseId;


    @JsonProperty("analysisStatusType")
    public String status;

    @JsonProperty("startedDateTime")
    public String startedDateTime;

    @JsonProperty("completedDateTime")
    public String completedDateTime;

    @JsonProperty("notes")
    public String notes;

    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class Scans extends FODEntityCollection<Scan> {

    }

}
