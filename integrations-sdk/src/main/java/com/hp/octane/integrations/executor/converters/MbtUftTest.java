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
package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.dto.executor.impl.TestingToolType;

import java.util.List;

public class MbtUftTest extends MbtTest {

    private String script;

    private String packageName;

    private List<String> underlyingTests;

    private List<Long> unitIds;

    private String encodedIterations;

    private List<String> functionLibraries;

    private List<String> recoveryScenarios;

    public MbtUftTest(String name, String packageName, String script, List<String> underlyingTests, List<Long> unitIds, String encodedIterations, List<String> functionLibraries,
                      List<String> recoveryScenarios) {
        super(name, TestingToolType.UFT);
        this.packageName = packageName;
        this.script = script;
        this.underlyingTests = underlyingTests;
        this.unitIds = unitIds;
        this.encodedIterations = encodedIterations;
        this.functionLibraries = functionLibraries;
        this.recoveryScenarios = recoveryScenarios;
    }

    public String getScript() {
        return script;
    }

    public String getPackage() {
        return packageName;
    }

    public List<String> getUnderlyingTests() {
        return underlyingTests;
    }

    public List<Long> getUnitIds() {
        return unitIds;
    }

    public String getEncodedIterations() {
        return encodedIterations;
    }

    public List<String> getFunctionLibraries() {
        return functionLibraries;
    }

    public List<String> getRecoveryScenarios() {
        return recoveryScenarios;
    }

}
