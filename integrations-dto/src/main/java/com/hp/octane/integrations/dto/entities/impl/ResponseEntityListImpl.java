package com.hp.octane.integrations.dto.entities.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hp.octane.integrations.dto.entities.ResponseEntityList;


public class ResponseEntityListImpl extends EntityListImpl implements ResponseEntityList {

    @JsonProperty(value="total_count")
    private int totalCount;

    @JsonProperty(value="exceeds_total_count")
    private boolean exceedsTotalCount;


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

}
