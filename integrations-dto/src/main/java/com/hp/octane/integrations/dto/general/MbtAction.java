package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOBase;

public interface MbtAction extends DTOBase {

    String getPathInScm();

    MbtAction setPathInScm(String pathInScm);

    long getUnitId();

    MbtAction setUnitId(long unitId);

    int getOrder();

    MbtAction setOrder(int order);
}
