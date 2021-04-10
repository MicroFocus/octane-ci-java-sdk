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

package com.hp.octane.integrations.dto.tests.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.hp.octane.integrations.dto.tests.BuildContext;

/**
 * Created by lev on 06/03/2016.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "build")
public class BuildContextImpl implements BuildContext {

    @JacksonXmlProperty(isAttribute = true, localName = "server_id")
    private String serverId;

    @JacksonXmlProperty(isAttribute = true, localName = "job_id")
    private String jobId;

    @JacksonXmlProperty(isAttribute = true, localName = "job_name")
    private String jobName;

    @JacksonXmlProperty(isAttribute = true, localName = "build_id")
    private String buildId;

    @JacksonXmlProperty(isAttribute = true, localName = "build_name")
    private String buildName;

    @JacksonXmlProperty(isAttribute = true, localName = "sub_type")
    private String subType;

    public String getServerId() {
        return serverId;
    }

    public BuildContext setServerId(String serverId) {
        this.serverId = serverId;
        return this;
    }

    public String getJobId() {
        return jobId;
    }

    public BuildContext setJobId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    public String getJobName() {
        return jobName;
    }

    public BuildContext setJobName(String jobName) {
        this.jobName = jobName;
        return this;
    }

    public String getBuildId() {
        return buildId;
    }

    public BuildContext setBuildId(String buildId) {
        this.buildId = buildId;
        return this;
    }

    public String getBuildName() {
        return buildName;
    }

    public BuildContext setBuildName(String buildName) {
        this.buildName = buildName;
        return this;
    }

    public String getSubType() {
        return subType;
    }

    public BuildContext setSubType(String subType) {
        this.subType = subType;
        return this;
    }

    @Override
    public String toString() {
        return "BuildContextImpl{" +
                "serverId='" + serverId + '\'' +
                ", jobId='" + jobId + '\'' +
                ", buildId='" + buildId + '\'' +
                ", subType='" + subType + '\'' +
                '}';
    }
}
