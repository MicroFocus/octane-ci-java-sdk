package com.hp.octane.integrations.executor.converters;

import java.util.List;

public class MbtTest {

    private String name;

    private String script;

    private String packageName;

    private List<String> underlyingTests;

    private List<Long> unitIds;

    private String encodedIterations;

    private List<String> functionLibraries;

    private List<String> recoveryScenarios;

    public MbtTest(String name, String packageName, String script, List<String> underlyingTests, List<Long> unitIds, String encodedIterations, List<String> functionLibraries,
                   List<String> recoveryScenarios) {
        this.name = name;
        this.packageName = packageName;
        this.script = script;
        this.underlyingTests = underlyingTests;
        this.unitIds = unitIds;
        this.encodedIterations = encodedIterations;
        this.functionLibraries = functionLibraries;
        this.recoveryScenarios = recoveryScenarios;
    }

    public String getName() {
        return name;
    }

    public String getScript() {
        return script;
    }

    public String getPackage() {
        return packageName;
    }

    public List<String> getUnderlyingTests() {
        return underlyingTests;
    }

    public List<Long> getUnitIds() {
        return unitIds;
    }

    public String getEncodedIterations() {
        return encodedIterations;
    }

    public List<String> getFunctionLibraries() {
        return functionLibraries;
    }

    public List<String> getRecoveryScenarios() {
        return recoveryScenarios;
    }
}
