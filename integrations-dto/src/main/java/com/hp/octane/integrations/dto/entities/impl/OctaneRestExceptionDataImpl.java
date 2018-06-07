package com.hp.octane.integrations.dto.entities.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.hp.octane.integrations.dto.entities.OctaneRestExceptionData;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaneRestExceptionDataImpl implements OctaneRestExceptionData {


    private String errorCode;
    private String correlationId;
    private String description;
    private String descriptionTranslated;
    private String stackTrace;
    private boolean businessError = false;


    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getDescriptionTranslated() {
        return descriptionTranslated;
    }

    @Override
    public String getStackTrace() {
        return stackTrace;
    }

    @Override
    public boolean getBusinessError() {
        return businessError;
    }

    @JsonSetter("error_code")
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @JsonSetter("correlation_id")
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonSetter("description_translated")
    public void setDescriptionTranslated(String descriptionTranslated) {
        this.descriptionTranslated = descriptionTranslated;
    }

    @JsonSetter("stack_trace")
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    @JsonSetter("business_error")
    public void setBusinessError(boolean businessError) {
        this.businessError = businessError;
    }
}
