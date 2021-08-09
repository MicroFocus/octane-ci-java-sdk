package com.hp.octane.integrations.uft.items;

import java.io.Serializable;

/**
 * @author Itay Karo on 02/08/2021
 */
public class UftTestParameter implements SupportsOctaneStatus, Serializable {

    private String name;

    private UftParameterDirection direction;

    private String defaultValue;

    private OctaneStatus octaneStatus;

    public UftTestParameter() {
        this.octaneStatus = OctaneStatus.NEW;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UftParameterDirection getDirection() {
        return direction;
    }

    public void setDirection(UftParameterDirection direction) {
        this.direction = direction;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public OctaneStatus getOctaneStatus() {
        return octaneStatus;
    }

    public void setOctaneStatus(OctaneStatus octaneStatus) {
        this.octaneStatus = octaneStatus;
    }

}
