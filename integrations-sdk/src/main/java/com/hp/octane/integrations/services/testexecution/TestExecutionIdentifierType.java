package com.hp.octane.integrations.services.testexecution;

public enum TestExecutionIdentifierType {
    SUITE("suite"), SUITE_RUN("suite run");

    private String name;

    TestExecutionIdentifierType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
