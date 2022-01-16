package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.dto.executor.impl.TestingToolType;

import java.io.Serializable;

/**
 * @author Itay Karo on 21/11/2021
 */
public abstract class MbtTest implements Serializable {

    private String name;

    private TestingToolType type;

    public MbtTest() {
    }

    public MbtTest(String name, TestingToolType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TestingToolType getType() {
        return type;
    }

    public void setType(TestingToolType type) {
        this.type = type;
    }

}
