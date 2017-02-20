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

package com.hp.octane.integrations.dto.tests;

import com.hp.octane.integrations.dto.DTOBase;

import java.util.List;

/**
 * TestsResult DTO.
 */

public interface TestsResult extends DTOBase {

	BuildContext getBuildContext();

	TestsResult setBuildContext(BuildContext buildContext);

	List<TestRun> getTestRuns();

	TestsResult setTestRuns(List<TestRun> testRuns);
}
