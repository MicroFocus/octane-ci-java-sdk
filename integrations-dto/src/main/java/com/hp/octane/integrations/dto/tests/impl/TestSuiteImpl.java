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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.hp.octane.integrations.dto.tests.Property;
import com.hp.octane.integrations.dto.tests.TestCase;
import com.hp.octane.integrations.dto.tests.TestSuite;

import java.util.List;

/**
 * Created by lev on 31/05/2016.
 */
@JacksonXmlRootElement(localName = "testsuite")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestSuiteImpl implements TestSuite {

    @JacksonXmlElementWrapper(localName = "properties")
    @JacksonXmlProperty(localName = "property")
    List<Property> properties;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "testcase")
    List<TestCase> testCases;

    public List<Property> getProperties() {
        return properties;
    }

    public TestSuite setProperties(List<Property> properties) {
        this.properties = properties;
        return this;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public TestSuite setTestCases(List<TestCase> testCases) {
        this.testCases = testCases;
        return this;
    }
}
