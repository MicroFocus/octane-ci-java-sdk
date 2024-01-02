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
