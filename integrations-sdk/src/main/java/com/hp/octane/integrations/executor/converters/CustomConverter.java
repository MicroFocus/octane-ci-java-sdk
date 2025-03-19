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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.hp.octane.integrations.utils.SdkStringUtils;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Converter to any given format
 */
public class CustomConverter extends TestsToRunConverter {

    private static final String $_PACKAGE = "$package";
    private static final String $_CLASS = "$class";
    private static final String $_TEST_NAME = "$testName";
    private CustomFormat customFormat;

    public CustomConverter() {
    }

    public CustomConverter(String format) {
        setFormatInternal(format);
    }

    @Override
    public TestsToRunConverter setFormat(String format) {
        //It is important to not override format for subclasses that assign format in constractor and later plugins can call to setFormat with empty/wrong string
        if (customFormat == null) {
            setFormatInternal(format);
        }

        return this;
    }

    protected TestsToRunConverter setFormatInternal(String format) {
        customFormat = buildCustomFormat(format);
        if (SdkStringUtils.isNotEmpty(customFormat.getTestsToRunConvertedParameterName())) {
            setTestsToRunConvertedParameterName(customFormat.getTestsToRunConvertedParameterName());
        }
        return this;
    }

    protected static CustomFormat buildCustomFormat(String format) {
        CustomFormat customFormat = new CustomFormat();
        String defaultErrorTemplate = "Field '%s' is missing in format json";
        Map<String, Object> parsed = parseJson(format, Map.class);
        customFormat.setAllowDuplication(getMapValue(parsed, "allowDuplication", false, true));
        customFormat.setTestPattern(getStringRequiredMapValue(parsed, "testPattern", defaultErrorTemplate));
        customFormat.setTestDelimiter(getMapValue(parsed, "testDelimiter", false, ""));
        customFormat.setPrefix(getMapValue(parsed, "prefix", false, ""));
        customFormat.setSuffix(getMapValue(parsed, "suffix", false, ""));
        customFormat.setTestsToRunConvertedParameterName(getMapValue(parsed, "testsToRunConvertedParameter", false, DEFAULT_TESTS_TO_RUN_CONVERTED_PARAMETER));

        List<Map<String, Object>> rawReplacement = (List<Map<String, Object>>) parsed.get("replacements");
        if (rawReplacement == null) {
            rawReplacement = Collections.emptyList();
        }
        rawReplacement.forEach(m -> {
            String replacementType = getMapValue(m, "type", true, null);
            String targetsRaw = getMapValue(m, "target", true, null);
            Set<String> targets = new HashSet<>(Arrays.asList(targetsRaw.split(Pattern.quote("|"))));
            targets.forEach(t -> {
                if (!t.startsWith("$")) {
                    throw new IllegalArgumentException(String.format("Illegal target '%s' in replacement '%s'. Target values must start with '$', for example $%s.", t, replacementType, t));
                }
            });

            ReplaceAction action;
            String errorMessage = null;
            switch (replacementType) {
                case "notLatinAndDigitToOctal":
                    action = new NotLatinAndDigitToOctal();
                    break;
                case "replaceRegex":
                    errorMessage = "The replacement 'replaceRegex' is missing field '%s'";
                    action = new ReplaceRegex()
                            .initialize(getStringRequiredMapValue(m, "regex", errorMessage),
                                    getStringRequiredMapValue(m, "replacement", errorMessage));
                    break;
                case "replaceRegexFirst":
                    errorMessage = "The replacement 'replaceRegexFirst' is missing field '%s'";
                    action = new ReplaceRegexFirst().initialize(getStringRequiredMapValue(m, "regex", errorMessage),
                            getStringRequiredMapValue(m, "replacement", errorMessage));
                    break;
                case "replaceString":
                    errorMessage = "The replacement 'replaceString' is missing field '%s'";
                    action = new ReplaceString().initialize(getStringRequiredMapValue(m, "string", errorMessage),
                            getStringRequiredMapValue(m, "replacement", errorMessage));
                    break;
                case "joinString":
                    action = new JoinString().initialize(getMapValue(m, "prefix", "", false, null),
                            getMapValue(m, "suffix", "", false, null));
                    break;
                case "toUpperCase":
                    action = new ToUpperCase();
                    break;
                case "toLowerCase":
                    action = new ToLowerCase();
                    break;
                default:
                    throw new IllegalArgumentException(String.format("Unknown replacement type '%s'", replacementType));
            }

            for (String t : targets) {
                if (!customFormat.getReplacements().containsKey(t)) {
                    customFormat.getReplacements().put(t, new ArrayList<>());
                }
                customFormat.getReplacements().get(t).add(action);
            }
        });
        return customFormat;
    }

    private static <T> T getMapValue(Map<String, Object> map, String fieldName, boolean required, T defaultValue) {
        return getMapValue(map, fieldName, defaultValue, required, "Field '%s' is missing in format json");
    }

    private static String getStringRequiredMapValue(Map<String, Object> map, String fieldName, String errorMessageTemplateIfNull) {
        return getMapValue(map, fieldName, "", true, errorMessageTemplateIfNull);
    }

    private static <T> T getMapValue(Map<String, Object> map, String fieldName, T defaultValue, boolean required, String errorMessageTemplateIfNull) {

        if (map.containsKey(fieldName)) {
            T obj = (T) map.get(fieldName);
            if (obj != null && defaultValue != null && (obj instanceof String) && !(defaultValue instanceof String)) {
                String suffix = "";
                if (defaultValue instanceof Boolean) {
                    suffix = ". Expected boolean value.";
                }
                throw new IllegalArgumentException("Illegal value for field " + fieldName + suffix);
            }
            return obj;
        } else {
            if (required) {
                throw new IllegalArgumentException(String.format(errorMessageTemplateIfNull, fieldName));
            } else {
                return defaultValue;
            }
        }

    }

    @Override
    public String convertInternal(List<TestToRunData> data, String executionDirectory, Map<String, String> globalParameters) {
        Set<String> existingKeys = new HashSet<>();
        addToSetIfPatterContains(existingKeys, $_PACKAGE);
        addToSetIfPatterContains(existingKeys, $_CLASS);
        addToSetIfPatterContains(existingKeys, $_TEST_NAME);
        data.stream().flatMap(t -> t.getParameters().keySet().stream()).map(param -> "$" + param).forEach(key -> addToSetIfPatterContains(existingKeys, key));

        Stream<String> stream = data.stream()
                .map(n -> convertToFormat(n, existingKeys))
                .filter(str -> str != null && !str.isEmpty());

        if (!customFormat.allowDuplication) {
            stream = stream.distinct();
        }
        String result = stream.collect(Collectors.joining(customFormat.getTestDelimiter(), customFormat.getPrefix(), customFormat.getSuffix()));
        return result;
    }

    private void addToSetIfPatterContains(Set<String> set, String key) {
        if (customFormat.getTestPattern().contains(key)) {
            set.add(key);
        }
    }

    protected String convertToFormat(TestToRunData testToRunData, Set<String> keysInTemplate) {

        int packageIndex = customFormat.getTestPattern().indexOf($_PACKAGE);

        String res = customFormat.getTestPattern();

        if (keysInTemplate.contains($_PACKAGE)) {
            String packageName = testToRunData.getPackageName();
            if (SdkStringUtils.isNotEmpty(packageName)) {
                res = res.replace($_PACKAGE, handleReplacements($_PACKAGE, packageName));
            } else {
                // remove $package part of format including its delimiter
                if (keysInTemplate.contains($_CLASS)) {
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

        if (keysInTemplate.contains($_CLASS)) {
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

        //replace parameters
        for (Map.Entry<String, String> entry : testToRunData.getParameters().entrySet()) {
            String replacementKey = "$" + entry.getKey();
            if (keysInTemplate.contains(replacementKey)) {
                res = res.replace(replacementKey, handleReplacements(replacementKey, entry.getValue()));
            }
        }

        //might be that the some parameter will be missing for some tests, but still exist in template, in such case it might remain after conversion as is.
        //this section set such values to be empty
        Set<String> existingParameters = testToRunData.getParameters().keySet();
        Set<String> missingParameters = keysInTemplate.stream().filter(key -> !existingParameters.contains(key)).filter(key -> !$_CLASS.equals(key) || !$_PACKAGE.equals(key) || !$_TEST_NAME.equals(key)).collect(Collectors.toSet());
        for (String missingParam : missingParameters) {
            res = res.replace(missingParam, "");
        }
        return res;
    }

    private String handleReplacements(String target, String str) {
        List<ReplaceAction> targetReplacements = customFormat.getReplacements().get(target);
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

    public static class CustomFormat {
        private String testDelimiter = "";
        private String testPattern = "";
        private String prefix = "";
        private String suffix = "";
        private boolean allowDuplication = true;
        private String testsToRunConvertedParameterName;
        private Map<String, List<ReplaceAction>> replacements = new HashMap<>();


        public String getTestDelimiter() {
            return testDelimiter;
        }

        public CustomFormat setTestDelimiter(String testDelimiter) {
            this.testDelimiter = testDelimiter;
            return this;
        }

        public String getTestPattern() {
            return testPattern;
        }

        public CustomFormat setTestPattern(String testPattern) {
            this.testPattern = testPattern;
            return this;
        }

        public String getSuffix() {
            return suffix;
        }

        public CustomFormat setSuffix(String suffix) {
            this.suffix = suffix;
            return this;
        }

        public Map<String, List<ReplaceAction>> getReplacements() {
            return replacements;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getTestsToRunConvertedParameterName() {
            return testsToRunConvertedParameterName;
        }

        public CustomFormat setTestsToRunConvertedParameterName(String testsToRunConvertedParameterName) {
            this.testsToRunConvertedParameterName = testsToRunConvertedParameterName;
            return this;
        }

        public boolean isAllowDuplication() {
            return allowDuplication;
        }

        public void setAllowDuplication(boolean allowDuplication) {
            this.allowDuplication = allowDuplication;
        }
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

    public static class ToUpperCase implements ReplaceAction {
        @Override
        public String replace(String string) {
            return string.toUpperCase();
        }
    }

    public static class ToLowerCase implements ReplaceAction {
        @Override
        public String replace(String string) {
            return string.toLowerCase();
        }
    }

    public static class JoinString implements ReplaceAction {
        private String prefix;
        private String suffix;

        public JoinString initialize(String prefix, String suffix) {
            this.prefix = prefix == null ? "" : prefix;
            this.suffix = suffix == null ? "" : suffix;
            return this;
        }

        @Override
        public String replace(String string) {
            return prefix + string + suffix;
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
