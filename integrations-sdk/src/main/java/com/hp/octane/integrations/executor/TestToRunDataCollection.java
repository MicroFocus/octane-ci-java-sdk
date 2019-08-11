package com.hp.octane.integrations.executor;

import java.util.ArrayList;
import java.util.List;

import static com.hp.octane.integrations.executor.TestToRunData.TESTS_TO_RUN_JSON_VERSION;

public class TestToRunDataCollection {

    String version = TESTS_TO_RUN_JSON_VERSION;
    List<TestToRunData> testsToRun = new ArrayList<>();

    public List<TestToRunData> getTestsToRun() {
        return testsToRun;
    }

    public String getVersion() {
        return version;
    }
}
