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
import com.hp.octane.integrations.dto.tests.TestCase;
import com.hp.octane.integrations.dto.tests.TestCaseFailure;

/**
 * Created by lev on 31/05/2016.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "testcase")
@JsonPropertyOrder({"name", "classname", "time", "failure"})
public class TestCaseImpl implements TestCase {

    @JacksonXmlProperty(isAttribute = true, localName = "name")
    private String name;

    @JacksonXmlProperty(isAttribute = true, localName = "time")
    private String time;

    @JacksonXmlProperty(isAttribute = true, localName = "status")
    private String status;

    @JacksonXmlProperty(isAttribute = true, localName = "classname")
    private String className;

    @JacksonXmlProperty(localName = "failure")
    private TestCaseFailure failure;

    public String getName() {
        return name;
    }

    public TestCase setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getTime() {
        return time;
    }

    @Override
    public TestCase setTime(String time) {
        this.time = time;
        return this;
    }

    @Override
    public String getTestStatus() {
        return status;
    }

    @Override
    public TestCase setTestStatus(String status) {
        this.status = status;
        return this;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public TestCase setClassName(String className) {
        this.className = className;
        return this;
    }

    @Override
    public TestCaseFailure getFailure() {
        return failure;
    }

    @Override
    public TestCase setFailure(TestCaseFailure failure) {
        this.failure = failure;
        return this;
    }
}
