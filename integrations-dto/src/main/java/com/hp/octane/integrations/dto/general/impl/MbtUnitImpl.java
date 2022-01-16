package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.dto.general.MbtUnit;
import com.hp.octane.integrations.dto.general.MbtUnitParameter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MbtUnitImpl implements MbtUnit {

    private String pathInScm;

    private String name;

    private long unitId;

    private int order;

    private List<MbtUnitParameter> parameters;

    private String testPath;

    private String actionName;

    private TestingToolType testingToolType;

    private String script;

    @Override
    public String getPathInScm() {
        return pathInScm;
    }

    @Override
    public MbtUnit setPathInScm(String pathInScm) {
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
    public MbtUnit setUnitId(long unitId) {
        this.unitId = unitId;
        return this;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public MbtUnit setOrder(int order) {
        this.order = order;
        return this;
    }

    @Override
    public List<MbtUnitParameter> getParameters() {
        return parameters;
    }

    @Override
    public MbtUnit setParameters(List<MbtUnitParameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    @Override
    public String getTestPath() {
        return testPath;
    }

    @Override
    public MbtUnit setTestPath(String testPath) {
        this.testPath = testPath;
        return this;
    }

    @Override
    public String getActionName() {
        return actionName;
    }

    @Override
    public MbtUnit setActionName(String actionName) {
        this.actionName = actionName;
        return this;
    }

    @Override
    public String getScript() {
        return script;
    }

    @Override
    public MbtUnit setScript(String script) {
        this.script = script;
        return this;
    }

    @Override
    public TestingToolType getTestingToolType() {
        return testingToolType;
    }

    @Override
    public MbtUnit setTestingToolType(TestingToolType testingToolType) {
        this.testingToolType = testingToolType;
        return this;
    }

}
