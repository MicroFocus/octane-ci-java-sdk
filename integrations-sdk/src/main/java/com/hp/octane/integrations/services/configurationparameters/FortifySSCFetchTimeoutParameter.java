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
package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;

public class FortifySSCFetchTimeoutParameter implements ConfigurationParameter {
    public static final String KEY = "FORTIFY_SSC_FETCH_TIMEOUT";
    public static final int DEFAULT_TIMEOUT = 12;
    private int timeout;

    private FortifySSCFetchTimeoutParameter(int timeout) {
        this.timeout = timeout;
    }


    public static FortifySSCFetchTimeoutParameter create(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter " + KEY + " : Expected non-empty integer value");
        }

        int value;
        try {
            value = Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter " + KEY + " : Expected integer value");
        }

        if (value <= 0 || value > 12) {
            throw new IllegalArgumentException("Parameter " + KEY + " : Expected integer value. The valid values are 1-12.");
        }

        return new FortifySSCFetchTimeoutParameter(value);
    }

    @Override
    public String getKey() {
        return KEY;
    }

    public int getTimeout() {
        return timeout;
    }

    @Override
    public String getRawValue() {
        return Integer.toString(timeout);
    }
}
