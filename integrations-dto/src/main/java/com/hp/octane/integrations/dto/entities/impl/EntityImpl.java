package com.hp.octane.integrations.dto.entities.impl;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.entities.EntityConstants;
import com.hp.octane.integrations.dto.entities.ResponseEntityList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityImpl implements Entity {

    private static final String COLLECTION_TOTAL_COUNT_FIELD = "total_count";
    private static final String COLLECTION_DATA_FIELD = "data";

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
            if (map.containsKey(EntityConstants.Base.TYPE_FIELD_NAME)) {
                myFieldValue = deserializeEntityFromMap(map);
            } else if (map.containsKey(COLLECTION_DATA_FIELD) && map.containsKey(COLLECTION_TOTAL_COUNT_FIELD)) {
                myFieldValue = deserializeEntityListFromMap(map);
            }
        }
        fields.put(fieldName, myFieldValue);

        return this;
    }

    private ResponseEntityList deserializeEntityListFromMap(Map<String, Object> map) {
        ResponseEntityList list = dtoFactory.newDTO(ResponseEntityList.class);
        list.setTotalCount((int) map.get(COLLECTION_TOTAL_COUNT_FIELD));
        List<Map<String, Object>> data = (List) map.get(COLLECTION_DATA_FIELD);
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
        return (String) getField(EntityConstants.Base.ID_FIELD);
    }


    @Override
    public Entity setId(String id) {
        setField(EntityConstants.Base.ID_FIELD, id);
        return this;
    }


    @Override
    public String getName() {
        return (String) getField(EntityConstants.Base.NAME_FIELD);
    }


    @Override
    public Entity setName(String name) {
        setField(EntityConstants.Base.NAME_FIELD, name);
        return this;
    }

    @Override
    public String getType() {
        return (String) getField(EntityConstants.Base.TYPE_FIELD_NAME);
    }


    @Override
    public Entity setType(String type) {
        setField(EntityConstants.Base.TYPE_FIELD_NAME, type);
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
    public Entity getEntityValue(String fieldName) {
        return (Entity) getField(fieldName);
    }

    @Override
    public Boolean getBooleanValue(String fieldName) {
        return (Boolean) getField(fieldName);
    }

    @Override
    public boolean containsField(String fieldName) {
        return getFields().containsKey(fieldName);
    }

    @Override
    public boolean containsFieldAndValue(String fieldName) {
        return getFields().containsKey(fieldName) && getFields().get(fieldName) != null;
    }


    @JsonValue
    public Map<String, Object> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (fields.containsKey(EntityConstants.Base.TYPE_FIELD_NAME)) {
            sb.append(getType());
        }

        if (fields.containsKey(EntityConstants.Base.TYPE_FIELD_NAME)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append("#");
            sb.append(getId());
        }

        if (fields.containsKey(EntityConstants.Base.NAME_FIELD)) {
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
