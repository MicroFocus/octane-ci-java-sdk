package com.hp.octane.integrations.dto.entities.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hp.octane.integrations.dto.entities.ResponseEntityList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseEntityListImpl extends EntityListImpl implements ResponseEntityList {

    @JsonProperty(value="total_count")
    private int totalCount;

    @JsonProperty(value="exceeds_total_count")
    private boolean exceedsTotalCount;

    @JsonProperty(value="total_error_count")
    private int totalErrorCount;

    @Override
    public int getTotalCount() {
        return totalCount;
    }

    @Override
    public boolean getExceedsTotalCount() {
        return exceedsTotalCount;
    }

    @Override
    public ResponseEntityList setTotalCount(int totalCount) {
        this.totalCount = totalCount;
        return this;
    }

    @Override
    public ResponseEntityList setExceedsTotalCount(boolean exceedsTotalCount) {
        this.exceedsTotalCount = exceedsTotalCount;
        return this;
    }

    @Override
    public int getTotalErrorCount() {
        return totalErrorCount;
    }

    @Override
    public ResponseEntityList setTotalErrorCount(int totalErrorCount) {
        this.totalErrorCount = totalErrorCount;
        return this;
    }
}
