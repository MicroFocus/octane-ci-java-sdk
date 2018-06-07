package com.hp.octane.integrations.dto.entities.impl;

import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.entities.EntityList;

import java.util.ArrayList;
import java.util.List;


public class EntityListImpl implements EntityList {

    private List<Entity> data;

    @Override
    public List<Entity> getData() {
        return data;
    }

    @Override
    public EntityList setData(List<Entity> data) {
        this.data = data;
        return this;
    }

    @Override
    public EntityList addEntity(Entity entity) {
        if (data == null) {
            data = new ArrayList<>();
        }
        data.add(entity);
        return this;
    }
}
