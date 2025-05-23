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
package com.hp.octane.integrations.executor;

import com.hp.octane.integrations.executor.converters.GradleConverter;
import com.hp.octane.integrations.executor.converters.JBehaveConverter;
import com.hp.octane.integrations.executor.converters.ProtractorConverter;
import com.hp.octane.integrations.utils.SdkStringUtils;

import java.io.Serializable;

public enum TestsToRunFramework implements Serializable {

    JUnit4("mvnSurefire", "JUnit/TestNG over Maven Surefire/Failsafe", ""),
    MF_UFT("uft", "Open Text UFT", ""),
    MF_MBT("mbt", "Open Text MBT", ""),
    CUCUMBER_JVM("cucumber_jvm", "Cucumber-JVM over Maven", ""),
    BDD_SCENARIO("bdd_scenario", "BDD Scenario", ""),
    JBehave("jbehave", "JBehave over Maven", JBehaveConverter.FORMAT),
    Protractor("protractor", "Protractor", ProtractorConverter.FORMAT),
    Gradle("gradle", "Gradle", GradleConverter.FORMAT),
    Custom("custom", "Custom", "");


    private final String value;
    private final String desc;
    protected final String format;

    TestsToRunFramework(String value, String desc, String format) {
        this.value = value;
        this.desc = desc;
        this.format = format;
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

}
