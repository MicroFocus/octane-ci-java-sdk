/*
 *     © 2017 EntIT Software LLC, a Micro Focus company, L.P.
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

import com.hp.octane.integrations.dto.tests.TestRun;
import com.hp.octane.integrations.dto.tests.TestRunError;
import com.hp.octane.integrations.dto.tests.TestRunResult;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

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
	private Long duration;

	@XmlAttribute(name = "started")
	private Long started;

	@XmlAnyElement(lax = true)
	private TestRunError error;

	@XmlAttribute(name = "external_report_url")
	private String externalReportUrl;

	public String getModuleName() {
		return moduleName;
	}

	public TestRun setModuleName(String moduleName) {
		this.moduleName = moduleName;
		return this;
	}

	public String getPackageName() {
		return packageName;
	}

	public TestRun setPackageName(String packageName) {
		this.packageName = packageName;
		return this;
	}

	public String getClassName() {
		return className;
	}

	public TestRun setClassName(String className) {
		this.className = className;
		return this;
	}

	public String getTestName() {
		return testName;
	}

	public TestRun setTestName(String testName) {
		this.testName = testName;
		return this;
	}

	public TestRunResult getResult() {
		return result;
	}

	public TestRun setResult(TestRunResult result) {
		this.result = result;
		return this;
	}

	public Long getDuration() {
		return duration;
	}

	public TestRun setDuration(Long duration) {
		this.duration = duration;
		return this;
	}

	public Long getStarted() {
		return started;
	}

	public TestRun setStarted(Long started) {
		this.started = started;
		return this;
	}

	public TestRunError getError() {
		return error;
	}

	public TestRun setError(TestRunError testError) {
		this.error = testError;
		return this;
	}

	public String getExternalReportUrl() {
		return externalReportUrl;
	}

	public TestRun setExternalReportUrl(String externalReportUrl) {
		this.externalReportUrl = externalReportUrl;
		return this;
	}
}
