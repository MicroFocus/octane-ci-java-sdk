package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOBase;

import java.util.List;

public interface MbtAction extends DTOBase {

    String getPathInScm();

    MbtAction setPathInScm(String pathInScm);

    String getName();

    void setName(String name);

    long getUnitId();

    MbtAction setUnitId(long unitId);

    int getOrder();

    MbtAction setOrder(int order);

    List<MbtActionParameter> getParameters();

    MbtAction setParameters(List<MbtActionParameter> parameters);

    String getTestPath();

    MbtAction setTestPath(String testPath);

    String getActionName();

    MbtAction setActionName(String actionName);
}
