package com.hp.octane.integrations.dto.entities.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.entities.OctaneBulkExceptionData;
import com.hp.octane.integrations.dto.entities.OctaneRestExceptionData;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaneBulkExceptionDataImpl implements OctaneBulkExceptionData {

    private List<OctaneRestExceptionData> errors;

    @Override
    public List<OctaneRestExceptionData> getErrors() {
        return errors;
    }

    public void setErrors(List<OctaneRestExceptionData> errors) {
        this.errors = errors;
    }
}
