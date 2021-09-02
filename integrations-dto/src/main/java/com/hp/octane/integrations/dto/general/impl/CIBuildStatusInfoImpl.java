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

package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.hp.octane.integrations.dto.snapshots.CIBuildStatus;
import com.hp.octane.integrations.dto.general.CIBuildStatusInfo;

/**
 * CIBuildStatusInfo DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class CIBuildStatusInfoImpl implements CIBuildStatusInfo {
    private CIBuildStatus buildStatus = CIBuildStatus.UNAVAILABLE;
    private String buildCiId;
    private String jobCiId;
    private String paramName;
    private String paramValue;
    private String exceptionMessage;
    private CIBuildResult buildResult = CIBuildResult.UNAVAILABLE;


    @Override
    public CIBuildStatus getBuildStatus() {
        return buildStatus;
    }

    @Override
    public String getBuildCiId() {
        return buildCiId;
    }

    @Override
    public CIBuildResult getBuildResult() {
        return buildResult;
    }

    @Override
    public CIBuildStatusInfo setBuildStatus(CIBuildStatus status) {
        this.buildStatus = status;
        return this;
    }

    @Override
    public CIBuildStatusInfo setBuildCiId(String buildCiId) {
        this.buildCiId = buildCiId;
        return this;
    }

    @Override
    public CIBuildStatusInfo setResult(CIBuildResult result) {
        this.buildResult = result;
        return this;
    }

    @Override
    public String getJobCiId() {
        return jobCiId;
    }

    @Override
    public CIBuildStatusInfo setJobCiId(String jobCiId) {
        this.jobCiId = jobCiId;
        return this;
    }

    @Override
    public String getParamName() {
        return paramName;
    }

    @Override
    public CIBuildStatusInfo setParamName(String fieldName) {
        this.paramName = fieldName;
        return this;
    }

    @Override
    public String getParamValue() {
        return paramValue;
    }

    @Override
    public CIBuildStatusInfo setParamValue(String fieldValue) {
        this.paramValue = fieldValue;
        return this;
    }

    @Override
    public String getExceptionMessage() {
        return exceptionMessage;
    }

    @Override
    public CIBuildStatusInfo setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
        return this;
    }
}
