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
 */

package com.hp.octane.integrations.dto.tests.impl;

import com.hp.octane.integrations.dto.tests.TestRun;
import com.hp.octane.integrations.dto.tests.TestRunError;
import com.hp.octane.integrations.dto.tests.TestRunResult;

import javax.xml.bind.annotation.*;

/**
 * TestRun DTO implementation.
 */

@XmlRootElement(name = "test_run")
@XmlAccessorType(XmlAccessType.NONE)
class TestRunImpl implements TestRun {

	@XmlAttribute(name = "module")
	private String moduleName;

	@XmlAttribute(name = "package")
	private String packageName;

	@XmlAttribute(name = "class")
	private String className;

	@XmlAttribute(name = "name")
	private String testName;

	@XmlAttribute(name = "status")
	private TestRunResult result;

	@XmlAttribute(name = "duration")
	private long duration;

	@XmlAttribute(name = "started")
	private long started;

	@XmlAnyElement(lax = true)
	private TestRunError error;

	@XmlAttribute(name = "external_report_url")
	private String externalReportUrl;

	@XmlAttribute(name = "external_test_id")
	private String externalTestId;

	@XmlAttribute(name = "external_run_id")
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
}
