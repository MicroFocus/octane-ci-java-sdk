package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.MbtAction;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MbtActionImpl implements MbtAction {

    private String pathInScm;
    private long unitId;
    private int order;

    @Override
    public String getPathInScm() {
        return pathInScm;
    }

    @Override
    public MbtAction setPathInScm(String pathInScm) {
        this.pathInScm = pathInScm;
        return this;
    }

    @Override
    public long getUnitId() {
        return unitId;
    }

    @Override
    public MbtAction setUnitId(long unitId) {
        this.unitId = unitId;
        return this;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public MbtAction setOrder(int order) {
        this.order = order;
        return this;
    }


}
