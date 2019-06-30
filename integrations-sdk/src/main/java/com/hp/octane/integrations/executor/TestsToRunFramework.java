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

import com.hp.octane.integrations.executor.converters.CucumberConverter;
import com.hp.octane.integrations.executor.converters.GradleConverter;
import com.hp.octane.integrations.executor.converters.ProtractorConverter;
import com.hp.octane.integrations.utils.SdkStringUtils;

public enum TestsToRunFramework {

    JUnit4("mvnSurefire","JUnit/TestNG over Maven Surefire/Failsafe", "", ""),
    MF_UFT("uft", "Micro Focus UFT", "", ""),
    Protractor("protractor", "Protractor", ProtractorConverter.FORMAT, ProtractorConverter.DELIMITER),
    Gradle("gradle","Gradle", GradleConverter.FORMAT, GradleConverter.DELIMITER),
    Cucumber("cucumber","Cucumber", CucumberConverter.FORMAT, CucumberConverter.DELIMITER),
    Custom("custom","Custom", "", "");

    private final String value;
    private final String desc;
    protected final String format;
    protected final String delimiter;

    TestsToRunFramework(String value, String desc, String format, String delimiter) {
        this.value = value;
        this.desc = desc;
        this.format = format;
        this.delimiter = delimiter;
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

    public String getFormat() {
        return format;
    }

    public String getDelimiter() {
        return delimiter;
    }
}
