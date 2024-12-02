/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hp.octane.integrations.dto.entities.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.hp.octane.integrations.dto.entities.OctaneRestExceptionData;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaneRestExceptionDataImpl implements OctaneRestExceptionData {

    private Integer index;
    private String errorCode;
    private String correlationId;
    private String description;
    private String descriptionTranslated;
    private String stackTrace;
    private boolean businessError = false;


    @Override
    public Integer getIndex() {
        return index;
    }

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

    @JsonSetter("index")
    public void setIndex(Integer index) {
        this.index = index;
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
