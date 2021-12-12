package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.dto.general.MbtDataTable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Itay Karo on 21/11/2021
 */
public class MbtCodelessTest extends MbtTest {

    private List<MbtCodelessUnit> units = new ArrayList<>();

    private MbtDataTable mbtDataTable;

    public MbtCodelessTest(String name) {
        super(name, TestingToolType.CODELESS);
    }

    public List<MbtCodelessUnit> getUnits() {
        return units;
    }

    public void setUnits(List<MbtCodelessUnit> units) {
        this.units = units;
    }

    public MbtDataTable getMbtDataTable() {
        return mbtDataTable;
    }

    public void setMbtDataTable(MbtDataTable mbtDataTable) {
        this.mbtDataTable = mbtDataTable;
    }

}
