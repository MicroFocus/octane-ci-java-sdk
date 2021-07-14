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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestToRunData implements Serializable {
    public static final String TESTS_TO_RUN_STRING_VERSION = "v1";
    public static final String TESTS_TO_RUN_JSON_VERSION = "v2";

    private String testName;
    private String className;
    private String packageName;
    private Map<String, String> parameters;


    public String getTestName() {
        return testName;
    }

    public TestToRunData setTestName(String testName) {
        this.testName = testName;
        return this;
    }

    public String getClassName() {
        return className;
    }

    public TestToRunData setClassName(String className) {
        this.className = className;
        return this;
    }

    public String getPackageName() {
        return packageName;
    }

    public TestToRunData setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public Map<String, String> getParameters() {
        if (parameters == null) {
            return Collections.emptyMap();
        }
        return parameters;
    }

    public TestToRunData addParameters(String key, String value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(key, value);
        return this;
    }

    public String getParameter(String key){
        if (parameters == null) {
            return null;
        }
        return parameters.get(key);
    }
}
