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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.hp.octane.integrations.dto.tests.TestCaseFailure;

/**
 * Created by lev on 31/05/2016.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "failure")
public class TestCaseFailureImpl implements TestCaseFailure {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlText
    private String stackTrace;


    @Override
    public String getType() {
        return type;
    }

    @Override
    public TestCaseFailure setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String getStackTrace() {
        return stackTrace;
    }

    @Override
    public TestCaseFailure setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
        return this;
    }
}
