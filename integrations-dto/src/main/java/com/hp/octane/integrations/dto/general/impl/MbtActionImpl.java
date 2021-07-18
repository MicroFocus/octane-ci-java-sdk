package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.MbtAction;
import com.hp.octane.integrations.dto.general.MbtActionParameter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MbtActionImpl implements MbtAction {

    private String pathInScm;
    private String name;
    private long unitId;
    private int order;
    private List<MbtActionParameter> parameters;
    private String testPath;
    private String actionName;

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
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
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

    @Override
    public List<MbtActionParameter> getParameters() {
        return parameters;
    }

    @Override
    public MbtAction setParameters(List<MbtActionParameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    @Override
    public String getTestPath() {
        return testPath;
    }

    @Override
    public MbtAction setTestPath(String testPath) {
        this.testPath = testPath;
        return this;
    }

    @Override
    public String getActionName() {
        return actionName;
    }

    @Override
    public MbtAction setActionName(String actionName) {
        this.actionName = actionName;
        return this;
    }
}
