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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.hp.octane.integrations.dto.tests.TestRun;
import com.hp.octane.integrations.dto.tests.TestRunError;
import com.hp.octane.integrations.dto.tests.TestRunResult;

/**
 * TestRun DTO implementation.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "test_run")
@JsonPropertyOrder({"moduleName", "packageName", "className", "testName", "result", "duration", "started",
        "externalTestId", "externalRunId", "externalReportUrl","run_type",
        "error", "description"})
class TestRunImpl implements TestRun {

    @JacksonXmlProperty(isAttribute = true, localName = "module")
    private String moduleName;

    //@JsonProperty("package")
    @JacksonXmlProperty(isAttribute = true, localName = "package")
    private String packageName;

    @JacksonXmlProperty(isAttribute = true, localName = "class")
    private String className;

    @JacksonXmlProperty(isAttribute = true, localName = "name")
    private String testName;

    @JacksonXmlProperty(isAttribute = true, localName = "status")
    private TestRunResult result;

    @JacksonXmlProperty(isAttribute = true, localName = "duration")
    private long duration;

    @JacksonXmlProperty(isAttribute = true, localName = "started")
    private long started;

    @JacksonXmlProperty(isAttribute = true, localName = "run_type")
    private String runType;

    @JsonProperty("error")
    //@XmlAnyElement(lax = true)
    private TestRunError error;

    @JsonProperty("description")
    private String description;

    @JacksonXmlProperty(isAttribute = true, localName = "external_report_url")
    private String externalReportUrl;

    @JacksonXmlProperty(isAttribute = true, localName = "external_test_id")
    private String externalTestId;

    @JacksonXmlProperty(isAttribute = true, localName = "external_run_id")
    private String externalRunId;

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public TestRun setModuleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public TestRun setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    @Override
    public String getRunType() {
        return runType;
    }

    @Override
    public TestRun setRunType(String runType) {
        this.runType = runType;
        return this;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public TestRun setClassName(String className) {
        this.className = className;
        return this;
    }

    @Override
    public String getTestName() {
        return testName;
    }

    @Override
    public TestRun setTestName(String testName) {
        if (testName == null || testName.length() == 0) {
            throw new IllegalArgumentException("TestName cannot be empty");
        }
        this.testName = testName;
        return this;
    }

    @Override
    public TestRunResult getResult() {
        return result;
    }

    @Override
    public TestRun setResult(TestRunResult result) {
        this.result = result;
        return this;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public TestRun setDuration(long duration) {
        if (started < 0) {
            throw new IllegalArgumentException("Duration must be a positive number");
        }
        this.duration = duration;
        return this;
    }

    @Override
    public long getStarted() {
        return started;
    }

    @Override
    public TestRun setStarted(long started) {
        if (started < 0) {
            throw new IllegalArgumentException("Started must be a positive number");
        }
        this.started = started;
        return this;
    }

    @Override
    public TestRunError getError() {
        return error;
    }

    @Override
    public TestRun setError(TestRunError testError) {
        this.error = testError;
        return this;
    }

    @Override
    public String getExternalReportUrl() {
        return externalReportUrl;
    }

    @Override
    public TestRun setExternalReportUrl(String externalReportUrl) {
        this.externalReportUrl = externalReportUrl;
        return this;
    }

    @Override
    public String getExternalRunId() {
        return externalRunId;
    }

    @Override
    public TestRun setExternalRunId(String externalRunId) {
        this.externalRunId = externalRunId;
        return this;
    }

    @Override
    public String getExternalTestId() {
        return externalTestId;
    }

    @Override
    public TestRun setExternalTestId(String externalTestId) {
        this.externalTestId = externalTestId;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public TestRun setDescription(String description) {
        this.description = description;
        return this;
    }
}
