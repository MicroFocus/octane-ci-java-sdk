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
package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.services.queueing.QueueingService;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class VulnerabilitiesQueueItem implements QueueingService.QueueItem {
    private String jobId;
    private String buildId;
    private long startTime;
    private long timeout;
    private String toolType;
    private boolean isRelevant;
    private Date baselineDate;
    private Map<String, String> additionalProperties = new HashMap<>();

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getToolType() {
        return toolType;
    }

    public void setToolType(String toolType) {
        this.toolType = toolType;
    }

    public boolean isRelevant() {
        return isRelevant;
    }

    public void setRelevant(boolean relevant) {
        this.isRelevant = relevant;
    }

    public Date getBaselineDate() {
        return baselineDate != null ? new Date(baselineDate.getTime()) : null;
    }

    public void setBaselineDate(Date baselineDate) {
        this.baselineDate = baselineDate != null ? new Date(baselineDate.getTime()) : null;
    }

    //  [YG] this constructor MUST be present, don't remove
    public VulnerabilitiesQueueItem() {
    }

    public VulnerabilitiesQueueItem(String jobId, String buildId) {
        if (jobId == null || jobId.isEmpty())
            throw new IllegalArgumentException("job ID MUST NOT be null nor empty");
        if (buildId == null || buildId.isEmpty())
            throw new IllegalArgumentException("build ID MUST NOT be null nor empty");

        this.jobId = jobId;
        this.buildId = buildId;
    }

    @Override
    public String toString() {
        return "'" + jobId + " #" + buildId + "'";
    }
}
