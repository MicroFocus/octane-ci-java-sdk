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
package com.hp.octane.integrations.services.pullrequestsandbranches.gitlab;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.services.pullrequestsandbranches.gitlab.pojo.Entity;
import com.hp.octane.integrations.services.pullrequestsandbranches.gitlab.pojo.EntityCollection;
import com.hp.octane.integrations.services.pullrequestsandbranches.gitlab.pojo.RequestErrors;
import com.hp.octane.integrations.services.pullrequestsandbranches.gitlab.pojo.ErrorDetails;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;

public class JsonConverter {
    private static final Logger logger = LogManager.getLogger(JsonConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T extends Entity> EntityCollection<T> convertCollection(String str, Class<T> entityType) throws JsonProcessingException {
        JavaType type = objectMapper.getTypeFactory().constructParametricType(EntityCollection.class, entityType);

        return objectMapper.readValue(str, type);
    }

    public static <T extends Entity> T convert(String str, Class<T> entityType) throws JsonProcessingException {
        return objectMapper.readValue(str, entityType);
    }

    public static String getErrorMessage(String jsonString) {
        try {
            RequestErrors requestErrors = objectMapper.readValue(jsonString, RequestErrors.class);
            if (!requestErrors.getErrors().isEmpty()) {
                return requestErrors.getErrors().stream().map(ErrorDetails::getMessage).collect(Collectors.joining("; "));
            }
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
        return jsonString;
    }
}
