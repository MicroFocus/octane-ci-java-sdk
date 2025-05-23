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
package com.hp.octane.integrations.services.entities;


import com.hp.octane.integrations.utils.SdkStringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created by berkovir on 17/05/2017.
 */
public class QueryHelper {

    public static String conditionRef(String name, long id) {
        return name + "={id=" + id + "}";
    }

    public static String conditionRef(String name, String refName, String value) {
        return name + "={" + condition(refName, value) + "}";
    }

    public static String conditionRefEmpty(String name) {
        return name + "={null}";
    }

    public static String conditionNot(String condition) {
        return "!" + condition;
    }

    public static String conditionStartWith(String name, String value) {
        return name + "='" + escapeQueryValue(value) + "*'";
    }

    public static String condition(String name, String value) {
        return name + "='" + escapeQueryValue(value) + "'";
    }

    public static String condition(String name, int value) {
        return name + "=" + value;
    }

    public static String conditionEmpty(String name) {
        return name + "=null";
    }

    public static String condition(String name, long value) {
        return name + "=" + value;
    }

    public static String condition(String name, boolean value) {
        return name + "=" + value;
    }

    public static String orConditions(String... conditions) {
        return "(" + Arrays.stream(conditions).map(c -> "(" + c + ")").collect(Collectors.joining("||")) + ")";
    }

    public static String conditionIn(String name, Collection<?> data, boolean isNumber) {
        if (isNumber) {
            return name + " IN " + SdkStringUtils.join(data, ",");
        } else {
            data = data.stream().map(o -> escapeQueryValue((String)o)).collect(Collectors.toList());
            //wrap values with '
            return name + " IN '" + SdkStringUtils.join(data, "','") + "'";
        }
    }

    private static String escapeQueryValue(String value) {
        return value.replaceAll("(\\\\)", "$1$1").replaceAll("([\"'()])", "\\\\$1");
    }
}
