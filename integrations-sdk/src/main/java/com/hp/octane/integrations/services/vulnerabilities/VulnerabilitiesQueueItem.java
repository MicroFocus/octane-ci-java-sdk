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


    public Map<String,Object> additionalProperties = new HashMap<>();

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

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
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
