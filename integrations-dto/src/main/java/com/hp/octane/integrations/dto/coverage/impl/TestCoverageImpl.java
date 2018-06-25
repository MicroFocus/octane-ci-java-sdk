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

package com.hp.octane.integrations.dto.coverage.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.coverage.FileCoverage;
import com.hp.octane.integrations.dto.coverage.TestCoverage;

/**
 * TestCoverage DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class TestCoverageImpl implements TestCoverage {
	private String testName;
	private String testClass;
	private String testPackage;
	private String testModule;
	private FileCoverage[] locs = new FileCoverage[0];

	public String getTestName() {
		return testName;
	}

	public TestCoverage setTestName(String testName) {
		this.testName = testName;
		return this;
	}

	public String getTestClass() {
		return testClass;
	}

	public TestCoverage setTestClass(String testClass) {
		this.testClass = testClass;
		return this;
	}

	public String getTestPackage() {
		return testPackage;
	}

	public TestCoverage setTestPackage(String testPackage) {
		this.testPackage = testPackage;
		return this;
	}

	public String getTestModule() {
		return testModule;
	}

	public TestCoverage setTestModule(String testModule) {
		this.testModule = testModule;
		return this;
	}

	public FileCoverage[] getLocs() {
		return locs;
	}

	public TestCoverage setLocs(FileCoverage[] locs) {
		this.locs = locs;
		return this;
	}
}
