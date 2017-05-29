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

package com.hp.octane.integrations.dto.executor.impl;

import com.hp.octane.integrations.dto.executor.TestExecutionInfo;

/**
 * Created by berkovir on 27/03/2017.
 */
public class TestExecutionInfoImpl implements TestExecutionInfo {
    private String testName;
    private String packageName;
    private String dataTable;

    @Override
    public String getTestName() {
        return testName;
    }

    @Override
    public TestExecutionInfo setTestName(String testName) {
        this.testName = testName;
        return this;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public TestExecutionInfo setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public String getDataTable() {
        return dataTable;
    }

    public void setDataTable(String dataTable) {
        this.dataTable = dataTable;
    }
}
