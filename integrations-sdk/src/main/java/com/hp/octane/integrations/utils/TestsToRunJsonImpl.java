package com.hp.octane.integrations.utils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hp.octane.integrations.executor.TestToRunData;

import java.util.ArrayList;
import java.util.List;

import static com.hp.octane.integrations.executor.TestToRunData.TESTS_TO_RUN_JSON_VERSION;

public class TestsToRunJsonImpl {

    List<TestToRunData> testsToRun = new ArrayList<>();
    String version = TESTS_TO_RUN_JSON_VERSION;

    @JsonDeserialize(contentAs=TestToRunData.class)
    public List<TestToRunData> rows = new ArrayList<>();

    public List<TestToRunData> getTestsToRun() {
        return testsToRun;
    }

    public String getVersion() {
        return version;
    }
}
