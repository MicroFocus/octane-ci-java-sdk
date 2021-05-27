package com.hp.octane.integrations.executor.converters;

import java.util.List;

public class MbtTest {

    private String name;

    private String script;

    private List<String> underlyingTests;

    private List<Long> unitIds;

    public MbtTest(String name, String script, List<String> underlyingTests, List<Long> unitIds) {
        this.name = name;
        this.script = script;
        this.underlyingTests = underlyingTests;
        this.unitIds = unitIds;
    }

    public String getName() {
        return name;
    }

    public String getScript() {
        return script;
    }

    public List<String> getUnderlyingTests() {
        return underlyingTests;
    }

    public List<Long> getUnitIds() {
        return unitIds;
    }
}
