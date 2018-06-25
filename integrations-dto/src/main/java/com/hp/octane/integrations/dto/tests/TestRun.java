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

package com.hp.octane.integrations.dto.tests;

import com.hp.octane.integrations.dto.DTOBase;

/**
 * TestRun DTO
 */

public interface TestRun extends DTOBase {

	String getModuleName();

	TestRun setModuleName(String moduleName);

	String getPackageName();

	TestRun setPackageName(String packageName);

	String getClassName();

	TestRun setClassName(String className);

	String getTestName();

	TestRun setTestName(String testName);

	TestRunResult getResult();

	TestRun setResult(TestRunResult result);

	Long getDuration();

	TestRun setDuration(Long duration);

	Long getStarted();

	TestRun setStarted(Long started);

	TestRunError getError();

	TestRun setError(TestRunError testError);

	String getExternalReportUrl();

	TestRun setExternalReportUrl(String externalReportUrl);
}
