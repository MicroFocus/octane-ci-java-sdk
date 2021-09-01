package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class BDDConverter extends TestsToRunConverter {

    private static final String FEATURE_FILE_PATH = "featureFilePath";

    @Override
    protected String convertInternal(List<TestToRunData> data, String executionDirectory, Map<String, String> globalParameters) {
        String featuresStr = data.stream()
                .map(d -> getFeatureFilePath(d)).filter(d -> d != null && !d.isEmpty())
                .distinct()
                .map(n -> "'" + n + "'")
                .collect(Collectors.joining(" "));
        String testsStr = data.stream()
                .map(d -> d.getTestName().replace("'","."))
                .map(n -> "--name '^" + n + "$'")
                .distinct()
                .collect(Collectors.joining(" "));
        return featuresStr + " " + testsStr;
    }

    private String getFeatureFilePath(TestToRunData item) {
        return item.getParameter(FEATURE_FILE_PATH);
    }
}
