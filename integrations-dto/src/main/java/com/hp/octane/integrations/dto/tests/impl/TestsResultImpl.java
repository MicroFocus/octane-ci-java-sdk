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

import com.hp.octane.integrations.dto.tests.BuildContext;
import com.hp.octane.integrations.dto.tests.TestField;
import com.hp.octane.integrations.dto.tests.TestsResult;
import com.hp.octane.integrations.dto.tests.TestRun;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * TestResult DTO implementation
 */

@XmlRootElement(name = "test_result")
@XmlAccessorType(XmlAccessType.NONE)
class TestsResultImpl implements TestsResult {

    @XmlAnyElement(lax = true)
    private BuildContext buildContext;

    @XmlElementWrapper(name = "test_fields")
    @XmlAnyElement(lax = true)
    private List<TestField> testFields;

    @XmlElementWrapper(name = "test_runs")
    @XmlAnyElement(lax = true)
    private List<TestRun> testRuns;

    public BuildContext getBuildContext() {
        return buildContext;
    }

    public TestsResult setBuildContext(BuildContext buildContext) {
        this.buildContext = buildContext;
        return this;
    }

    public List<TestRun> getTestRuns() {
        return testRuns;
    }

    public TestsResult setTestRuns(List<TestRun> testRuns) {
        this.testRuns = testRuns;
        return this;
    }

    @Override
    public List<TestField> getTestFields() {
        return testFields;
    }

    @Override
    public TestsResult setTestFields(List<TestField> testFields) {
        this.testFields = testFields;
        return this;
    }
}
