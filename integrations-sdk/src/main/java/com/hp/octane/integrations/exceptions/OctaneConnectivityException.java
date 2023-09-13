/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
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

package com.hp.octane.integrations.exceptions;

public class OctaneConnectivityException extends ErrorCodeBasedException {

    public static final String AUTHORIZATION_FAILURE_MESSAGE = "Authorization failure. Validate that Client ID is assigned to 'CI/CD Integration' role.";
    public static final String CONN_SHARED_SPACE_INVALID_MESSAGE = "Unable to connect the shared space";
    public static final String UNEXPECTED_FAILURE_MESSAGE = "Connectivity test failed with unexpected failure" ;
    public static final String UNSUPPORTED_SDK_VERSION_MESSAGE = "This plugin version is outdated and is not supported by ALM Octane." ;

    public static final String AUTHENTICATION_FAILURE_KEY = "AUTHENTICATION_FAILURE";
    public static final String AUTHORIZATION_FAILURE_KEY = "AUTHORIZATION_FAILURE";
    public static final String CONN_SHARED_SPACE_INVALID_KEY = "CONNECTION_SHARED_SPACE_INVALID";
    public static final String UNEXPECTED_FAILURE_KEY = "UNEXPECTED_FAILURE";
    public static final String UNSUPPORTED_SDK_VERSION_KEY = "UNSUPPORTED_SDK_VERSION";

    private String errorMessageKey;
    private String errorMessageVal;

    public static final String AUTHENTICATION_FAILURE_MESSAGE = "Authentication failure. Check credentials or server location. If using proxy, check that your proxy settings are correct";
    public OctaneConnectivityException(int errorCode, String errorMessageKey, String errorMessageVal) {
        super(errorCode);

        this.errorMessageKey = errorMessageKey;
        this.errorMessageVal = errorMessageVal;
    }

    public String getErrorMessageKey() {
        return errorMessageKey;
    }

    public void setErrorMessageKey(String errorMessageKey) {
        this.errorMessageKey = errorMessageKey;
    }

    public String getErrorMessageVal() {
        return errorMessageVal;
    }

    public void setErrorMessageVal(String errorMessageVal) {
        this.errorMessageVal = errorMessageVal;
    }

}
