package com.hp.octane.integrations.dto.entities.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.entities.OctaneBulkException;
import com.hp.octane.integrations.dto.entities.OctaneException;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaneBulkExceptionImpl extends RuntimeException implements OctaneBulkException {

    private List<OctaneException> errors;

    @Override
    public List<OctaneException> getErrors() {
        return errors;
    }

    public void setErrors(List<OctaneException> errors) {
        this.errors = errors;
    }
}
