/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
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
package com.hp.octane.integrations.services.vulnerabilities.ssc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hp.octane.integrations.services.vulnerabilities.RawVulnerability;
import com.hp.octane.integrations.services.vulnerabilities.ssc.SSCToOctaneIssueUtil;

/**
 * Created by hijaziy on 7/24/2018.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Issues extends SscBaseEntityArray<Issues.Issue> {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Issue implements RawVulnerability {
        @JsonProperty("analyzer")
        public String analyzer;
        @JsonProperty("audited")
        public Boolean audited;
        @JsonProperty("bugURL")
        public String bugURL;

        @JsonProperty("folderGuid")
        public String folderGuid;
        @JsonProperty("folderId")
        public Integer folderId;

        @JsonProperty("fullFileName")
        public String fullFileName;

        @JsonProperty("id")
        public Integer id;

        @JsonProperty("issueInstanceId")
        public String issueInstanceId;
        @JsonProperty("issueName")
        public String issueName;
        @JsonProperty("package")
        public String package1;
        @JsonProperty("issueStatus")
        public String issueStatus;
        @JsonProperty("kingdom")
        public String kingdom;
        @JsonProperty("lastScanId")
        public Integer lastScanId;

        @JsonProperty("lineNumber")
        public Integer lineNumber;
        @JsonProperty("primaryLocation")
        public String primaryLocation;

        @JsonProperty("reviewed")
        public Boolean reviewed;

        @JsonProperty("scanStatus")
        public String scanStatus;
        //@JsonProperty("friority")
        @JsonProperty("severity")
        public Integer severity;


        @JsonProperty("likelihood")
        public String likelihood;
        @JsonProperty("impact")
        public String impact;
        @JsonProperty("confidence")
        public String confidance;
        @JsonProperty("removedDate")
        public String removedDate;
        @JsonProperty("removed")
        public Boolean removed;
        @JsonProperty("foundDate")
        public String foundDate;
        @JsonProperty("_href")
        public String hRef;
        @JsonProperty("primaryTag")
        public String analysis;

        @Override
        public boolean isNew() {
            return scanStatus != null &&
                    scanStatus.equalsIgnoreCase(SSCToOctaneIssueUtil.STATUS_NEW);
        }
        @Override
        public String getRemoteId() {
            return issueInstanceId;
        }
    }
}
