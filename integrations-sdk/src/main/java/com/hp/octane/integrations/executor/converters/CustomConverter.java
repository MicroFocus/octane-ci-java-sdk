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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.hp.octane.integrations.utils.SdkStringUtils;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*
 * Converter to any given format
 */
public class CustomConverter extends TestsToRunConverter {

    private static final String $_PACKAGE = "$package";
    private static final String $_CLASS = "$class";
    private static final String $_TEST_NAME = "$testName";
    private static final Set<String> allowedTargets = new HashSet(Arrays.asList($_PACKAGE, $_CLASS, $_TEST_NAME));

    protected String delimiter = "";
    protected String testPattern = "";
    protected String prefix = "";
    protected String suffix = "";
    Map<String, List<ReplaceAction>> replacements = new HashMap<>();

    public CustomConverter() {
    }

    public CustomConverter(String format, String delimiter) {
        initialize(format, delimiter);
    }

    private void initialize(String format, String delimiter) {
        this.testPattern = format;
        this.delimiter = delimiter;
        if (format.trim().startsWith("{")) {
            String defaultErrorTemplate = "Field '%s' is missing in format json";
            Map<String, Object> parsed = parseJson(testPattern, Map.class);
            this.testPattern = getMapValue(parsed, "testPattern", defaultErrorTemplate);
            this.delimiter = getMapValue(parsed, "testDelimiter", false, "");
            this.prefix = getMapValue(parsed, "prefix", false, "");
            this.suffix = getMapValue(parsed, "suffix", false, "");
            String testsToRunConvertedParameterName = (getMapValue(parsed, "testsToRunConvertedParameter", false, DEFAULT_TESTS_TO_RUN_CONVERTED_PARAMETER));
            setTestsToRunConvertedParameterName(testsToRunConvertedParameterName);

            List<Map<String, Object>> rawReplacement = (List<Map<String, Object>>) parsed.get("replacements");
            if (rawReplacement == null) {
                rawReplacement = Collections.emptyList();
            }
            rawReplacement.forEach(m -> {
                String replacementType = getMapValue(m, "type", true, null);
                String targetsRaw = getMapValue(m, "target", true, null);
                Set<String> targets = new HashSet<>(Arrays.asList(targetsRaw.split(Pattern.quote("|"))));
                targets.forEach(t -> {
                    if (!(allowedTargets.contains(t))) {
                        throw new IllegalArgumentException(String.format("Illegal target '%s' in replacement '%s'. Allowed values : %s", t, replacementType, allowedTargets));
                    }
                });

                ReplaceAction action = null;
                String errorMessage = null;
                switch (replacementType) {
                    case "notLatinAndDigitToOctal":
                        action = new NotLatinAndDigitToOctal();
                        break;
                    case "replaceRegex":
                        errorMessage = "The replacement 'replaceRegex' is missing field '%s'";
                        action = new ReplaceRegex()
                                .initialize(getMapValue(m, "regex", errorMessage),
                                        getMapValue(m, "replacement", errorMessage));
                        break;
                    case "replaceRegexFirst":
                        errorMessage = "The replacement 'replaceRegexFirst' is missing field '%s'";
                        action = new ReplaceRegexFirst().initialize(getMapValue(m, "regex", errorMessage),
                                getMapValue(m, "replacement", errorMessage));
                        break;
                    case "replaceString":
                        errorMessage = "The replacement 'replaceString' is missing field '%s'";
                        action = new ReplaceString().initialize(getMapValue(m, "string", errorMessage),
                                getMapValue(m, "replacement", errorMessage));
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("Unknown replacement type '%s'", replacementType));
                }

                for (String t : targets) {
                    if (!replacements.containsKey(t)) {
                        replacements.put(t, new ArrayList<>());
                    }
                    replacements.get(t).add(action);
                }
            });
        }
    }

    private String getMapValue(Map<String, Object> map, String fieldName, boolean required, String defaultValue) {
        return getMapValue(map, fieldName, defaultValue, required, "Field '%s' is missing in format json");
    }

    private String getMapValue(Map<String, Object> map, String fieldName, String errorMessageTemplateIfNull) {
        return getMapValue(map, fieldName, "", true, errorMessageTemplateIfNull);
    }

    private String getMapValue(Map<String, Object> map, String fieldName, String defaultValue, boolean required, String errorMessageTemplateIfNull) {
        if (map.containsKey(fieldName)) {
            return (String) map.get(fieldName);
        } else {
            if (required) {
                throw new IllegalArgumentException(String.format(errorMessageTemplateIfNull, fieldName));
            } else {
                return defaultValue == null ? "" : defaultValue;
            }
        }
    }

    @Override
    public String convert(List<TestToRunData> data, String executionDirectory) {
        String collect = data.stream()
                .map(n -> convertToFormat(n))
                .distinct()
                .collect(Collectors.joining(delimiter, prefix, suffix));
        return collect;
    }

    @Override
    public TestsToRunConverter setProperties(Map<String, String> properties) {
        initialize(properties.get(CONVERTER_FORMAT), properties.get(CONVERTER_DELIMITER));
        return this;
    }

    protected String convertToFormat(TestToRunData testToRunData) {
        boolean patternContainsPackage = testPattern.contains($_PACKAGE);
        boolean patternContainsClass = testPattern.contains($_CLASS);
        int packageIndex = testPattern.indexOf($_PACKAGE);

        String res = testPattern;

        if (patternContainsPackage) {
            String packageName = testToRunData.getPackageName();
            if (SdkStringUtils.isNotEmpty(packageName)) {
                res = res.replace($_PACKAGE, handleReplacements($_PACKAGE, packageName));
            } else {
                // remove $package part of format including its delimiter
                if (patternContainsClass) {
                    // the $class expresion exists in given format - remove the part till $class
                    //      for example: the format is XXXX$package.||.$class.||.$testName
                    //      the result: XXXX$class.||.$testName
                    res = splice(res, packageIndex, res.indexOf($_CLASS));
                } else {
                    // no $class expresion exists in given format - remove the part till $testName
                    //      for example: the format is XXXX$package.||.$testName
                    //      the result: XXXX$testName
                    res = splice(res, packageIndex, res.indexOf($_TEST_NAME));
                }
            }
        }

        if (patternContainsClass) {
            String className = testToRunData.getClassName();
            if (SdkStringUtils.isNotEmpty(className)) {
                res = res.replace($_CLASS, handleReplacements($_CLASS, className));
            } else {
                // remove $class part of format including its delimiter (till $testName)
                //      for example: the format is XXXX$class.||.$testName
                //      the result: XXXX$testName
                res = splice(res, res.indexOf($_CLASS), res.indexOf($_TEST_NAME));
            }
        }

        res = res.replace($_TEST_NAME, handleReplacements($_TEST_NAME, testToRunData.getTestName()));
        return res;
    }

    private String handleReplacements(String target, String str) {
        List<ReplaceAction> targetReplacements = replacements.get(target);
        if (targetReplacements == null) {
            return str;
        } else {
            String myStr = str;
            for (ReplaceAction action : targetReplacements) {
                myStr = action.replace(myStr);
            }
            return myStr;
        }
    }

    /**
     * method changes the contents of a string by removing existing elements form index to index
     *
     * @param string     the original string
     * @param beginIndex the begin index to remove the characters
     * @param endIndex   the begin index to remove the characters
     * @return a new string contains the a part of the given string without existing substring form begin to end
     */
    private String splice(String string, int beginIndex, int endIndex) {
        return string.substring(0, beginIndex) + string.substring(endIndex);
    }

    private static <T> T parseJson(String content, Class<T> valueType) {
        final ObjectMapper mapper = new ObjectMapper();

        try {
            T value = mapper.readValue(content, valueType);
            return value;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse :" + e.getMessage(), e);
        }
    }

    public interface ReplaceAction {
        String replace(String string);

    }

    public static class ReplaceString implements ReplaceAction {
        private String str;
        private String replacement;

        public ReplaceString initialize(String str, String replacement) {
            this.str = str;
            this.replacement = replacement;
            return this;
        }

        @Override
        public String replace(String string) {
            return string.replace(str, replacement);
        }
    }

    public static class ReplaceRegex implements ReplaceAction {
        private String regex;
        private String replacement;

        public ReplaceRegex initialize(String regex, String replacement) {
            this.regex = regex;
            this.replacement = replacement;
            return this;
        }

        @Override
        public String replace(String string) {
            return string.replaceAll(regex, replacement);
        }
    }

    public static class ReplaceRegexFirst implements ReplaceAction {
        private String regex;
        private String replacement;

        public ReplaceRegexFirst initialize(String regex, String replacement) {
            this.regex = regex;
            this.replacement = replacement;
            return this;
        }

        @Override
        public String replace(String string) {
            return string.replaceFirst(regex, replacement);
        }
    }

    public static class NotLatinAndDigitToOctal implements ReplaceAction {
        @Override
        public String replace(String str) {
            StringBuilder sb = new StringBuilder(str.length());
            for (char c : str.toCharArray()) {
                if (isLatinLetterOrDigit(c)) {
                    sb.append(c);
                } else {
                    String octalRepresentation = String.format("\\%03o", (int) c);
                    sb.append(octalRepresentation);
                }
            }
            return sb.toString();
        }

        private boolean isLatinLetterOrDigit(char c) {
            return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
        }
    }

}
