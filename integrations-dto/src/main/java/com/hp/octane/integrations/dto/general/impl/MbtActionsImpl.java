package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.MbtAction;
import com.hp.octane.integrations.dto.general.MbtActions;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MbtActionsImpl implements MbtActions {

    List<MbtAction> actions;

    @Override
    public MbtActions setActions(List<MbtAction> actions){
        this.actions=actions;
        return this;
    }

    @Override
    public List<MbtAction> getActions() {
        return actions;
    }
}
