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
package com.hp.octane.integrations.octaneExecution;

import com.hp.octane.integrations.executor.converters.GradleConverter;
import com.hp.octane.integrations.executor.converters.JBehaveConverter;
import com.hp.octane.integrations.executor.converters.ProtractorConverter;
import com.hp.octane.integrations.utils.SdkStringUtils;

import java.io.Serializable;

public enum ExecutionMode implements Serializable {

    SUITE_RUNS_IN_OCTANE("suite_runs_in_octane", "Execute suite runs in ALM Octane"),
    SUITE_IN_CI("suites_in_ci", "Get tests from suites and trigger execution jobs"),
    FAVORITES_IN_CI("favorites_in_ci", "Get tests from favorites and trigger execution jobs");

    private final String value;
    private final String desc;

    ExecutionMode(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public String value() {
        return value;
    }

    public String description() {
        return desc;
    }

    public static ExecutionMode fromValue(String value) {
        if (SdkStringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("value MUST NOT be null nor empty");
        }

        for (ExecutionMode v : values()) {
            if (v.value.equals(value)) {
                return v;
            }
        }

        throw new IllegalStateException("Framework '" + value + "' is not supported");
    }

}
