package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOBase;

import java.util.List;

public interface MbtData extends DTOBase {

    List<MbtAction> getActions();

    MbtData setActions(List<MbtAction> actions);

    MbtDataTable getData();

    MbtData setData(MbtDataTable dataTable);
}
