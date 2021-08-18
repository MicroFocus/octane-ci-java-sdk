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
 * SCMRepository DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class CIBuildStatusInfoImpl implements CIBuildStatusInfo {
    private CIBuildStatus buildStatus = CIBuildStatus.UNAVAILABLE;
    private String buildCiId;
    private CIBuildResult result = CIBuildResult.UNAVAILABLE;


    @Override
    public CIBuildStatus getBuildStatus() {
        return buildStatus;
    }

    @Override
    public String getBuildCiId() {
        return buildCiId;
    }

    @Override
    public CIBuildResult getResult() {
        return result;
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
        this.result = result;
        return this;
    }
}
