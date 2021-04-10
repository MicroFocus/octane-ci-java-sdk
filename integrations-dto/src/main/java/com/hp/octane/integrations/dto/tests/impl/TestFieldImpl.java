/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.octane.integrations.dto.tests.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.hp.octane.integrations.dto.tests.TestField;

/**
 * TestRun DTO implementation.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "test_field")
@JsonPropertyOrder({"type", "value"})
class TestFieldImpl implements TestField {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String fieldType;

    @JacksonXmlProperty(isAttribute = true, localName = "value")
    private String fieldValue;

    @Override
    public String getType() {
        return fieldType;
    }

    @Override
    public TestField setType(String fieldType) {
        this.fieldType = fieldType;
        return this;
    }

    @Override
    public String getValue() {
        return fieldValue;
    }

    @Override
    public TestField setValue(String fieldValue) {
        this.fieldValue = fieldValue;
        return this;
    }
}
