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
 */

package com.hp.octane.integrations.executor;

import com.hp.octane.integrations.executor.converters.CustomConverter;
import org.junit.Assert;
import org.junit.Test;

/**
 * Octane SDK tests
 */

public class CustomConverterWithJsonTest {

    private static final String fullFormatRawData = "v1:MFA.simpleA.tests|AppTestA|myTestA;MFA.simpleA.tests|AppTestB|test Send";

    @Test
    public void jsonConverterTest() {
        String json = "{" +
                "\"testPattern\": \"$package.$class#$testName\"," +
                "\"testDelimiter\": \"||\"," +
                "\"prefix\":\"[\"," +
                "\"suffix\":\"]\"," +
                "\"testsToRunConvertedParameter\": \"converted\"," +
                "\"replacements\": [" +
                "{\"type\":\"notLatinAndDigitToOctal\",\"target\":\"$testName\"}," +
                "{\"type\":\"replaceRegex\",\"target\":\"$package\",\"regex\":\"MF.\",\"replacement\":\"MF\"}," +
                "{\"type\":\"replaceString\",\"target\":\"$package\",\"string\":\"simpleA\",\"replacement\":\"simple\"}," +
                "{\"type\":\"replaceRegexFirst\",\"target\":\"$class\",\"regex\":\"AppTestA\",\"replacement\":\"AppTest\"}," +
                "{\"type\":\"replaceString\", \"target\":\"$testName\",\"string\":\"myTestA\",\"replacement\":\"myTest\"}" +
                "]}";

        CustomConverter converter = new CustomConverter(json);
        TestsToRunConverterResult result = converter.convert(fullFormatRawData, "");

        Assert.assertEquals("converted", result.getTestsToRunConvertedParameterName());
        Assert.assertEquals("[MF.simple.tests.AppTest#myTest||MF.simple.tests.AppTestB#test\\040Send]", result.getConvertedTestsString());
    }

    @Test
    public void wrongTypeInMultipleTypesTest() {
        String json = "{" +
                "\"testPattern\": \"$package.$class#$testName\"," +
                "\"testDelimiter\": \"+\"," +
                "\"replacements\": [" +
                "{\"type\":\"replaceString\",\"target\":\"$package|class\",\"string\":\"simpleA\",\"replacement\":\"simple\"}" +
                "]}";

        try {
            CustomConverter converter = new CustomConverter(json);
            Assert.fail("Exception must have been thrown, but it not.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Illegal target 'class' in replacement 'replaceString'. Target values must start with '$', for example $class.", e.getMessage());
        }
    }

    @Test
    public void replaceStringInMultipleTypesTest() {
        String json = "{" +
                "\"testPattern\": \"$package.$class#$testName\"," +
                "\"testDelimiter\": \"+\"," +
                "\"replacements\": [" +
                "{\"type\":\"replaceString\",\"target\":\"$package|$class|$testName\",\"string\":\"test\",\"replacement\":\"bubu\"}" +
                "]}";

        CustomConverter converter = new CustomConverter(json);
        String actual = converter.convert(fullFormatRawData, "").getConvertedTestsString();

        Assert.assertEquals("MFA.simpleA.bubus.AppTestA#myTestA+MFA.simpleA.bubus.AppTestB#bubu Send", actual);
    }

    @Test
    public void replaceToUpperCaseAndToLowerCaseTest() {
        String json = "{" +
                "\"testPattern\": \"$package=$class#$testName\"," +
                "\"testDelimiter\": \"+\"," +
                "\"replacements\": [" +
                "{\"type\":\"toLowerCase\",\"target\":\"$class\"}," +
                "{\"type\":\"toUpperCase\",\"target\":\"$testName\"}" +
                "]}";

        CustomConverter converter = new CustomConverter(json);
        String actual = converter.convert(fullFormatRawData, "").getConvertedTestsString();

        Assert.assertEquals("MFA.simpleA.tests=apptesta#MYTESTA+MFA.simpleA.tests=apptestb#TEST SEND", actual);
    }

    @Test
    public void joinStringWithDuplicationTest() {
        String json = "{" +
                "\"testPattern\": \"$package\"," +
                "\"replacements\": [" +
                "{\"type\":\"joinString\",\"target\":\"$package\",\"prefix\":\"prefix|\",\"suffix\":\"|suffix;\"}" +
                "]}";

        CustomConverter converter = new CustomConverter(json);
        String actual = converter.convert(fullFormatRawData, "").getConvertedTestsString();

        Assert.assertEquals("prefix|MFA.simpleA.tests|suffix;prefix|MFA.simpleA.tests|suffix;", actual);
    }

    @Test
    public void joinStringWithoutDuplicationTest() {
        String json = "{" +
                "\"allowDuplication\": \"false\"," +
                "\"testPattern\": \"$package\"," +
                "\"replacements\": [" +
                "{\"type\":\"joinString\",\"target\":\"$package\",\"prefix\":\"prefix|\",\"suffix\":\"|suffix;\"}" +
                "]}";

        CustomConverter converter = new CustomConverter(json);
        String actual = converter.convert(fullFormatRawData, "").getConvertedTestsString();

        Assert.assertEquals("prefix|MFA.simpleA.tests|suffix;", actual);
    }

    @Test
    public void replaceRegexIgnoreCaseInMultipleTypesTest() {
        String json = "{" +
                "\"testPattern\": \"$package.$class#$testName\"," +
                "\"testDelimiter\": \"+\"," +
                "\"replacements\": [" +
                "{\"type\":\"replaceRegex\",\"target\":\"$package|$class|$testName\",\"regex\":\"(?i)test\",\"replacement\":\"bubu\"}" +
                "]}";

        CustomConverter converter = new CustomConverter(json);
        String actual = converter.convert(fullFormatRawData, "").getConvertedTestsString();

        Assert.assertEquals("MFA.simpleA.bubus.AppbubuA#mybubuA+MFA.simpleA.bubus.AppbubuB#bubu Send", actual);
    }

    @Test
    public void convertExternalTestId() {
        String singleRawDataWithExternalTest = "v1:MF.simple.tests|AppTest|testAlways|externalTestId=bubu";
        String json = "{" +
                "\"testPattern\": \"$package.$class#$testName-$externalTestId\"," +
                "\"testDelimiter\": \"+\"," +
                "\"replacements\": [" +
                "{\"type\":\"toUpperCase\",\"target\":\"$externalTestId\"}" +
                "]}";

        CustomConverter converter = new CustomConverter(json);
        String actual = converter.convert(singleRawDataWithExternalTest, "").getConvertedTestsString();

        Assert.assertEquals("MF.simple.tests.AppTest#testAlways-BUBU", actual);
    }

    @Test
    public void convertExternalTestIdAndClearMissingValue() {
        String singleRawDataWithExternalTest = "v1:MF.simple.tests|AppTest|testAlways|externalTestId=bubu;MF.simple.tests|AppTest|testNotAlways";
        String json = "{" +
                "\"testPattern\": \"$package.$class#$testName$externalTestId\"," +
                "\"testDelimiter\": \"+\"," +
                "\"replacements\": [" +
                "{\"type\":\"joinString\",\"target\":\"$externalTestId\",\"prefix\":\"-\"}," +
                "{\"type\":\"toUpperCase\",\"target\":\"$externalTestId\"}" +
                "]}";

        CustomConverter converter = new CustomConverter(json);
        String actual = converter.convert(singleRawDataWithExternalTest, "").getConvertedTestsString();

        Assert.assertEquals("MF.simple.tests.AppTest#testAlways-BUBU+MF.simple.tests.AppTest#testNotAlways", actual);
    }

    @Test
    public void missingTestPatternTest() {
        String json = "{" +
                "\"testDelimiter\": \"||\"}";
        try {
            CustomConverter converter = new CustomConverter(json);
            Assert.fail("Exception must have been thrown, but it not.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Field 'testPattern' is missing in format json", e.getMessage());
        }
    }

    @Test
    public void illegalReplaceActionTypeTest() {
        String json = "{" +
                "\"testPattern\": \"$package.$class#$testName\"," +
                "\"replacements\": [" +
                "{\"type\":\"notExist\",\"target\":\"$package\",\"regex\":\"MF.\",\"replacement\":\"MF\"}" +
                "]}";
        try {
            CustomConverter converter = new CustomConverter(json);
            Assert.fail("Exception must have been thrown, but it not.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Unknown replacement type 'notExist'", e.getMessage());
        }
    }

    @Test
    public void illegalTargetInReplacementTest() {
        String json = "{" +
                "\"testPattern\": \"$package.$class#$testName\"," +
                "\"replacements\": [" +
                "{\"type\":\"replaceRegex\",\"target\":\"package\",\"regex\":\"MF.\",\"replacement\":\"MF\"}" +
                "]}";
        try {
            CustomConverter converter = new CustomConverter(json);
            Assert.fail("Exception must have been thrown, but it not.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Illegal target 'package' in replacement 'replaceRegex'. Target values must start with '$', for example $package.", e.getMessage());
        }
    }

    @Test
    public void missingRegexFieldInReplaceRegexTest() {
        String json = "{" +
                "\"testPattern\": \"$package.$class#$testName\"," +
                "\"replacements\": [" +
                "{\"type\":\"replaceRegex\",\"target\":\"$package\",\"regex1\":\"MF.\",\"replacement\":\"MF\"}" +
                "]}";
        try {
            CustomConverter converter = new CustomConverter(json);
            Assert.fail("Exception must have been thrown, but it not.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("The replacement 'replaceRegex' is missing field 'regex'", e.getMessage());
        }
    }

    @Test
    public void missingReplacementFieldInReplaceRegexTest() {
        String json = "{" +
                "\"testPattern\": \"$package.$class#$testName\"," +
                "\"replacements\": [" +
                "{\"type\":\"replaceRegex\",\"target\":\"$package\",\"regex\":\"MF.\",\"replacement1\":\"MF\"}" +
                "]}";
        try {
            CustomConverter converter = new CustomConverter(json);
            Assert.fail("Exception must have been thrown, but it not.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("The replacement 'replaceRegex' is missing field 'replacement'", e.getMessage());
        }
    }


}
