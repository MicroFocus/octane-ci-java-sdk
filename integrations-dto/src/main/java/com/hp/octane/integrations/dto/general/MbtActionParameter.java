package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOBase;

public interface MbtActionParameter extends DTOBase {

    String getParameterId();

    void setParameterId(String id);

    String getName();

    MbtActionParameter setName(String name);

    String getType();

    MbtActionParameter setType(String type);

    int getOrder();

    MbtActionParameter setOrder(int order);

    String getOutputParameter();

    MbtActionParameter setOutputParameter(String outputParameter);
}
