package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.dto.general.MbtData;
import com.hp.octane.integrations.dto.general.MbtDataTable;
import com.hp.octane.integrations.dto.general.MbtUnit;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MbtDataImpl implements MbtData {

    List<MbtUnit> units;

    MbtDataTable dataTable;

    @Override
    @JsonProperty("actions")
    public MbtData setUnits(List<MbtUnit> units) {
        this.units = units;
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
    public List<MbtUnit> getUnits() {
        return units;
    }

    @Override
    public TestingToolType getTestingToolType() {
        if (getUnits() == null || getUnits().isEmpty()) {
            return TestingToolType.UNKNOWN;
        }
        return getUnits().get(0).getTestingToolType();
    }

}
