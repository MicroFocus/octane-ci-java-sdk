/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hp.octane.integrations.dto.securityscans.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.dto.entities.Entity;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaneIssueImpl implements OctaneIssue {

    private Map extended_data;
    private String primary_location_full;
    private Integer line;
    private Entity analysis;
    private Entity state;
    private Entity severity;
    private String remote_id;
    private String introduced_date;
    private String external_link;
    private String tool_name;
    private String category;
    private String package1;
    private String remote_tag;
    private String owner_email;

    @Override
    @JsonProperty(value = "extended_data")
    public void setExtendedData(Map extendedData) {
        this.extended_data = extendedData;
    }

    @Override
    @JsonProperty(value = "primary_location_full")
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
    @JsonProperty(value = "extended_data")
    public Map getExtendedData() {
        return this.extended_data;
    }

    @Override
    @JsonProperty(value = "primary_location_full")
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

    @Override
    @JsonProperty(value = "remote_id")
    public String getRemoteId() {
        return remote_id;
    }

    @Override
    @JsonProperty(value = "remote_id")
    public void setRemoteId(String remote_id) {
        this.remote_id = remote_id;
    }

    @Override
    @JsonProperty(value = "introduced_date")
    public String getIntroducedDate() {
        return this.introduced_date;
    }

    @Override
    @JsonProperty(value = "introduced_date")
    public void setIntroducedDate(String introducedDate) {
        this.introduced_date = introducedDate;
    }

    @Override
    @JsonProperty(value = "external_link")
    public String getExternalLink() {
        return this.external_link;
    }

    @Override
    @JsonProperty(value = "external_link")
    public void setExternalLink(String external_link) {
        this.external_link = external_link;
    }

    @Override
    @JsonProperty(value = "tool_name")
    public String getToolName() {
        return tool_name;
    }

    @Override
    @JsonProperty(value = "tool_name")
    public void setToolName(String toolName) {
        this.tool_name = toolName;
    }

    @Override
    @JsonProperty(value = "category")
    public String getCategory() {
        return category;
    }

    @Override
    @JsonProperty(value = "category")
    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    @JsonProperty(value = "package")
    public String getPackage() {
        return package1;
    }

    @Override
    @JsonProperty(value = "package")
    public void setPackage(String package1) {
        this.package1 = package1;
    }


    @Override
    @JsonProperty(value = "remote_tag")
    public String getRemoteTag(){
        return remote_tag;
    }

    @Override
    @JsonProperty(value = "remote_tag")
    public void setRemoteTag(String remoteTag){
        this.remote_tag = remoteTag;
    }

    @Override
    @JsonProperty(value = "owner_email")
    public String getOwnerEmail() {
        return owner_email;
    }

    @Override
    @JsonProperty(value = "owner_email")
    public void setOwnerEmail(String ownerEmail){
        this.owner_email = ownerEmail;
    }

    @Override
    public String toString() {
        return "remote_id=" + remote_id;
    }
}
