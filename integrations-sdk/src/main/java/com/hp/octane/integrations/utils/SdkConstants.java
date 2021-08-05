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

package com.hp.octane.integrations.utils;

public class SdkConstants {

    public static class FileSystem {
        public static final String WINDOWS_PATH_SPLITTER = "\\";
        public static final String LINUX_PATH_SPLITTER = "/";
    }

    public static class General {
        public static final String JOB_PARENT_DELIMITER = ";";

        public static final String INSTANCE_ID_TO_BE_SET_IN_SDK = "to-be-filled-in-SDK";
        public static final String MQM_TESTS_FILE_NAME = "mqmTests.xml";
    }

    public static class JobParameters {
        public static final String SUITE_ID_PARAMETER_NAME = "suiteId";
        public static final String SUITE_RUN_ID_PARAMETER_NAME = "suiteRunId";
        public static final String OCTANE_WORKSPACE_PARAMETER_NAME = "octaneWorkspaceId";
        public static final String OCTANE_SPACE_PARAMETER_NAME = "octaneSpaceId";
        public static final String EXECUTION_ID_PARAMETER_NAME = "executionId";
        public static final String OCTANE_CONFIG_ID_PARAMETER_NAME = "octaneConfigId";
        public static final String ADD_GLOBAL_PARAMETERS_TO_TESTS_PARAM = "ADD_GLOBAL_PARAMETERS_TO_TESTS";
    }
}
