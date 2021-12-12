package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.MbtUnitParameter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MbtUnitParameterImpl implements MbtUnitParameter {

    private String id;

    private String name;

    private String type;

    private int order;

    private String outputParameter;

    private String originalName; // parameter name before merge

    private String unitParameterId; // source unit parameter id

    private String unitParameterName; // source unit parameter name

    @Override
    public String getParameterId() {
        return id;
    }

    @Override
    public void setParameterId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MbtUnitParameter setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public MbtUnitParameter setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public MbtUnitParameter setOrder(int order) {
        this.order = order;
        return this;
    }

    @Override
    public String getOutputParameter() {
        return outputParameter;
    }

    @Override
    public MbtUnitParameter setOutputParameter(String outputParameter) {
        this.outputParameter = outputParameter;
        return this;
    }

    @Override
    public String getOriginalName() {
        return originalName;
    }

    @Override
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    @Override
    public String getUnitParameterId() {
        return unitParameterId;
    }

    @Override
    public void setUnitParameterId(String unitParameterId) {
        this.unitParameterId = unitParameterId;
    }

    @Override
    public String getUnitParameterName() {
        return unitParameterName;
    }

    @Override
    public void setUnitParameterName(String unitParameterName) {
        this.unitParameterName = unitParameterName;
    }

}
