package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.executor.impl.TestingToolType;

import java.util.List;

public interface MbtUnit extends DTOBase {

    String getPathInScm();

    MbtUnit setPathInScm(String pathInScm);

    String getName();

    void setName(String name);

    long getUnitId();

    MbtUnit setUnitId(long unitId);

    int getOrder();

    MbtUnit setOrder(int order);

    List<MbtUnitParameter> getParameters();

    MbtUnit setParameters(List<MbtUnitParameter> parameters);

    String getTestPath();

    MbtUnit setTestPath(String testPath);

    String getActionName();

    MbtUnit setActionName(String actionName);

    String getScript();

    MbtUnit setScript(String script);

    TestingToolType getTestingToolType();

    MbtUnit setTestingToolType(TestingToolType testingToolType);

}
