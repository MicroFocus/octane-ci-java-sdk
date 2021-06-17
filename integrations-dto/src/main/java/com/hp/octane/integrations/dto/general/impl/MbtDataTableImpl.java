package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.MbtDataTable;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MbtDataTableImpl implements MbtDataTable {

    List<String> parameters;
    List<List<String>> iterations;

    @Override
    public List<String> getParameters() {
        return parameters;
    }

    @Override
    public MbtDataTable setParameters(List<String> parameters) {
        this.parameters = parameters;
        return this;
    }

    @Override
    public List<List<String>> getIterations() {
        return iterations;
    }

    @Override
    public MbtDataTable setIterations(List<List<String>> iterations) {
        this.iterations = iterations;
        return this;
    }
}
