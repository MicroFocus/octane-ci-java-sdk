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

package com.hp.octane.integrations.dto.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * ItemType enum
 */

public enum ItemType {
    JOB("Job"),
    MULTI_BRANCH("Multi branch"),
    FOLDER("Folder");

    private String value;

    ItemType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ItemType fromValue(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("value MUST NOT be null nor empty");
        }

        ItemType result = JOB;
        for (ItemType v : values()) {
            if (v.value.compareTo(value) == 0) {
                result = v;
                break;
            }
        }
        return result;
    }
}