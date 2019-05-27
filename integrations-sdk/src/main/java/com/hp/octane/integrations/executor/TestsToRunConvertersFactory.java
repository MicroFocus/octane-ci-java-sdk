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

import com.hp.octane.integrations.executor.converters.*;


public class TestsToRunConvertersFactory {

    public static TestsToRunConverter createConverter(TestsToRunFramework framework) {
        switch (framework) {
            case JUnit4:
                return new MavenSurefireAndFailsafeConverter();
            case MF_UFT:
                return new MfUftConverter();
            case Protractor:
                return new ProtractorConverter();
            case Gradle:
                return new GradleConverter();
            case Custom:
                return new CustomConverter();
            default:
                throw new UnsupportedOperationException(framework.name() + " framework does not have supported converter");
        }

    }
}
