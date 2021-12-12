package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOBase;

import java.io.Serializable;

public interface MbtUnitParameter extends DTOBase, Serializable {

    String getParameterId();

    void setParameterId(String id);

    String getName();

    MbtUnitParameter setName(String name);

    String getType();

    MbtUnitParameter setType(String type);

    int getOrder();

    MbtUnitParameter setOrder(int order);

    String getOutputParameter();

    MbtUnitParameter setOutputParameter(String outputParameter);

    String getOriginalName();

    void setOriginalName(String originalName);

    String getUnitParameterId();

    void setUnitParameterId(String unitParameterId);

    String getUnitParameterName();

    void setUnitParameterName(String unitParameterName);

}
