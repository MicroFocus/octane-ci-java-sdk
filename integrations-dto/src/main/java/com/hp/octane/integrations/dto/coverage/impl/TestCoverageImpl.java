/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
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
