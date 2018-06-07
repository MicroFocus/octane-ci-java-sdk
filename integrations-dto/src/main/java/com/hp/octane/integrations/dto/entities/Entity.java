/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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

package com.hp.octane.integrations.dto.entities;

import com.hp.octane.integrations.dto.DTOBase;

/**
 * Entity DTO
 */

public interface Entity extends DTOBase {

    String TYPE_FIELD_NAME = "type";
    String ID_FIELD_NAME = "id";
    String NAME_FIELD_NAME = "name";
    String LOGICAL_NAME_FIELD_NAME = "logical_name";

    Object getField(String fieldName);

    Entity setField(String fieldName, Object fieldValue);

    String getId();

    Entity setId(String id);

    String getName();

    Entity setName(String name);

    String getLogicalName();

    String getType();

    Entity setType(String type);

    String getStringValue(String fieldName);

    Long getLongValue(String fieldName);

    Boolean getBooleanValue(String fieldName);

    boolean containsField(String fieldName);

}
