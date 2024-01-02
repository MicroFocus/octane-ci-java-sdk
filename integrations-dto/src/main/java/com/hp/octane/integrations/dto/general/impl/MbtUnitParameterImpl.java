/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
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
