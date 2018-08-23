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
package com.hp.octane.integrations.dto.securityscans.impl;

import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.dto.entities.Entity;

import java.util.Map;

public class OctaneIssueImpl implements OctaneIssue {

    private Map extented_data;
    private String primary_location_full;
    private Integer line;
    private Entity analysis;
    private Entity state;
    private Entity severity;
    private String remote_id;
    private String introduced_date;
    private String external_link;
    private String toolName;

    @Override
    public void setExtendedData(Map extendedData) {
        this.extented_data = extendedData;
    }

    @Override
    public void setPrimaryLocationFull(String primaryLocationFull) {
        this.primary_location_full = primaryLocationFull;
    }

    @Override
    public void setLine(Integer line) {
        this.line = line;
    }

    @Override
    public void setAnalysis(Entity analysis) {
        this.analysis = analysis;
    }

    @Override
    public void setState(Entity state) {
        this.state = state;
    }

    @Override
    public void setSeverity(Entity severity) {
        this.severity = severity;
    }

    @Override
    public Map getExtendedData() {
        return this.extented_data;
    }

    @Override
    public String getPrimaryLocationFull() {
        return this.primary_location_full;
    }

    @Override
    public Integer getLine() {
        return this.line;
    }

    @Override
    public Entity getAnalysis() {
        return analysis;
    }

    @Override
    public Entity getState() {
        return state;
    }

    @Override
    public Entity getSeverity() {
        return severity;
    }

    public String getRemoteId() {
        return remote_id;
    }

    public void setRemoteId(String remote_id) {
        this.remote_id = remote_id;
    }

    @Override
    public String getIntroducedDate() {
        return this.introduced_date;
    }

    @Override
    public void setIntroducedDate(String introducedDate) {
        this.introduced_date = introducedDate;
    }

    @Override
    public String getExternalLink() {
        return this.external_link;
    }

    @Override
    public void setExternalLink(String external_link) {
        this.external_link = external_link;
    }

    @Override
    public String getToolName() {
        return toolName;
    }

    @Override
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
}
