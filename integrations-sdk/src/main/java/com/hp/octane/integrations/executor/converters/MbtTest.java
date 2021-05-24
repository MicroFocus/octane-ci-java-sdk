package com.hp.octane.integrations.executor.converters;

import java.util.List;

public class MbtTest {

    private String name;

    private String script;

    private List<String> underlyingTests;

    public MbtTest(String name, String script, List<String> underlyingTests) {
        this.name = name;
        this.script = script;
        this.underlyingTests = underlyingTests;
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
}
