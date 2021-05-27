package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOBase;

import java.util.List;

public interface MbtActions extends DTOBase {

    List<MbtAction> getActions();

    MbtActions setActions(List<MbtAction> actions);

}
