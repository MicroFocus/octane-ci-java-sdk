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
package com.hp.octane.integrations.executor;

import com.hp.octane.integrations.util.SdkStringUtils;

public enum TestsToRunFramework {

    JUnit4("junit4","JUnit4.7+/TestNG for Maven (requires Maven Surefire Plugin v2.19.1+) "),
    //TestNG("testNg","TestNG for Maven"),
    MF_UFT("uft", "Micro Focus UFT (mtbx)");

    private final String value;
    private final String desc;

    TestsToRunFramework(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public String value() {
        return value;
    }

    public static TestsToRunFramework fromValue(String value) {
        if (SdkStringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("value MUST NOT be null nor empty");
        }

        for (TestsToRunFramework v : values()) {
            if (v.value.equals(value)) {
                return v;
            }
        }

        throw new IllegalStateException("Framework '" + value + "' is not supported");
    }

    public String getDesc() {
        return desc;
    }
}
