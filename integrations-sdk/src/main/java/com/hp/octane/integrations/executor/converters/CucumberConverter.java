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

package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.executor.TestToRunData;

/*
 * Converter to format : mvn clean test -Dcucumber.options="--name (scenario1|scenario2)"
 */
public class CucumberConverter extends CustomConverter {

    public static final String FORMAT = "$testName";
    public static final String DELIMITER = "|";

    public CucumberConverter() {
        super(FORMAT, DELIMITER);
    }

    @Override
    protected String convertToFormat(TestToRunData testToRunData) {
        String superResult = super.convertToFormat(testToRunData);
        return replaceNonLatinLettersToOctalRepresentation(superResult);
    }

    public static String replaceNonLatinLettersToOctalRepresentation(String str) {

        StringBuilder sb = new StringBuilder(str.length());
        for (char c : str.toCharArray()) {
            if (isLatinLetterOrDigit(c)) {
                sb.append(c);
            } else {
                String octalRepresentation = String.format("\\%03o", (int)c);
                sb.append(octalRepresentation);
            }
        }
        return sb.toString();
    }

    public static boolean isLatinLetterOrDigit(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }
}
