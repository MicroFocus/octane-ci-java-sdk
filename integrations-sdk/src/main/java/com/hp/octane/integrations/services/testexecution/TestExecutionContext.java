package com.hp.octane.integrations.services.testexecution;

import com.hp.octane.integrations.dto.entities.Entity;

public class TestExecutionContext {

    private Entity testRunner;
    private String testsToRun;
    private TestExecutionIdentifierType identifierType;
    private String identifier;

    public TestExecutionContext(Entity testRunner, String testsToRun, TestExecutionIdentifierType identifierType, String identifier) {
        this.testRunner = testRunner;
        this.identifierType = identifierType;
        this.testsToRun = testsToRun;
        this.identifier = identifier;
    }

    public Entity getTestRunner() {
        return testRunner;
    }

    public TestExecutionIdentifierType getIdentifierType() {
        return identifierType;
    }

    public String getTestsToRun() {
        return testsToRun;
    }

    public String getIdentifier() {
        return identifier;
    }

}
