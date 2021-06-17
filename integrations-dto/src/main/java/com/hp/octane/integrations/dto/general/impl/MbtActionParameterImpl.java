package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.MbtActionParameter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MbtActionParameterImpl implements MbtActionParameter {

    private String id;

    private String name;

    private String type;

    private int order;

    private String outputParameter;

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
    public MbtActionParameter setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public MbtActionParameter setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public MbtActionParameter setOrder(int order) {
        this.order = order;
        return this;
    }

    @Override
    public String getOutputParameter() {
        return outputParameter;
    }

    @Override
    public MbtActionParameter setOutputParameter(String outputParameter) {
        this.outputParameter = outputParameter;
        return this;
    }
}
