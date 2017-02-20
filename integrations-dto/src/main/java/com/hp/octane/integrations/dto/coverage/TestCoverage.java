/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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

package com.hp.octane.integrations.dto.coverage;

import com.hp.octane.integrations.dto.DTOBase;

/**
 * TestCoverage DTO
 */

public interface TestCoverage extends DTOBase {

	String getTestName();

	TestCoverage setTestName(String testName);

	String getTestClass();

	TestCoverage setTestClass(String testClass);

	String getTestPackage();

	TestCoverage setTestPackage(String testPackage);

	String getTestModule();

	TestCoverage setTestModule(String testModule);

	FileCoverage[] getLocs();

	TestCoverage setLocs(FileCoverage[] locs);
}
