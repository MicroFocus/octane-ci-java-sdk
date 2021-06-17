package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.MbtAction;
import com.hp.octane.integrations.dto.general.MbtData;
import com.hp.octane.integrations.dto.general.MbtDataTable;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MbtDataImpl implements MbtData {

    List<MbtAction> actions;
    MbtDataTable dataTable;

    @Override
    public MbtData setActions(List<MbtAction> actions) {
        this.actions = actions;
        return this;
    }

    @Override
    public MbtDataTable getData() {
        return dataTable;
    }

    @Override
    public MbtData setData(MbtDataTable dataTable) {
        this.dataTable = dataTable;
        return this;
    }

    @Override
    public List<MbtAction> getActions() {
        return actions;
    }
}
