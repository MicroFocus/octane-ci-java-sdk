package com.hp.octane.integrations.dto.entities.impl;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.entities.ResponseEntityList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityImpl implements Entity {

    private Map<String, Object> fields = new HashMap<>();
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();

    @Override
    public Object getField(String fieldName) {
        return fields.get(fieldName);
    }

    @JsonAnySetter
    @Override
    public Entity setField(String fieldName, Object fieldValue) {
        Object myFieldValue = fieldValue;
        if (fieldValue instanceof Map) {
            Map<String, Object> map = (Map) fieldValue;
            if (map.containsKey(TYPE_FIELD_NAME)) {
                myFieldValue = deserializeEntityFromMap(map);
            } else if (map.containsKey("data") && map.containsKey("total_count")) {
                myFieldValue = deserializeEntityListFromMap(map);
            }

        }
        fields.put(fieldName, myFieldValue);

        return this;
    }

    private ResponseEntityList deserializeEntityListFromMap(Map<String, Object> map) {
        ResponseEntityList list = dtoFactory.newDTO(ResponseEntityList.class);
        list.setTotalCount((int) map.get("total_count"));
        List<Map<String, Object>> data = (List) map.get("data");
        for (Map<String, Object> entry : data) {
            Entity entity = deserializeEntityFromMap(entry);
            list.addEntity(entity);
        }
        return list;
    }

    private Entity deserializeEntityFromMap(Map<String, Object> map) {

        Entity entity = dtoFactory.newDTO(Entity.class);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            entity.setField(entry.getKey(), entry.getValue());
        }
        return entity;
    }


    @Override
    public String getId() {
        return (String) getField(ID_FIELD_NAME);
    }


    @Override
    public Entity setId(String id) {
        setField(ID_FIELD_NAME, id);
        return this;
    }


    @Override
    public String getName() {
        return (String) getField(NAME_FIELD_NAME);
    }


    @Override
    public Entity setName(String name) {
        setField(NAME_FIELD_NAME, name);
        return this;
    }

    @Override
    public String getLogicalName() {
        return (String) getField(LOGICAL_NAME_FIELD_NAME);
    }

    @Override
    public String getType() {
        return (String) getField(TYPE_FIELD_NAME);
    }


    @Override
    public Entity setType(String type) {
        setField(TYPE_FIELD_NAME, type);
        return this;
    }

    @Override
    public String getStringValue(String fieldName) {
        return (String) getField(fieldName);
    }

    @Override
    public Long getLongValue(String fieldName) {
        return (Long) getField(fieldName);
    }

    @Override
    public Boolean getBooleanValue(String fieldName) {
        return (Boolean) getField(fieldName);
    }

    @Override
    public boolean containsField(String fieldName) {
        return getFields().containsKey(fieldName);
    }


    @JsonValue
    public Map<String, Object> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (fields.containsKey(TYPE_FIELD_NAME)) {
            sb.append(getType());
        }

        if (fields.containsKey(ID_FIELD_NAME)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append("#");
            sb.append(getId());
        }

        if (fields.containsKey(NAME_FIELD_NAME)) {
            if (sb.length() > 0) {
                sb.append(" - ");
            }
            sb.append(getName());
        }

        if (sb.length() > 0) {
            return sb.toString();
        } else {
            return super.toString();
        }
    }
}
