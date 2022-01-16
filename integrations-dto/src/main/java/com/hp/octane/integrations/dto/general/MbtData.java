package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.executor.impl.TestingToolType;

import java.util.List;

public interface MbtData extends DTOBase {

    List<MbtUnit> getUnits();

    MbtData setUnits(List<MbtUnit> units);

    MbtDataTable getData();

    MbtData setData(MbtDataTable dataTable);

    TestingToolType getTestingToolType();

}
