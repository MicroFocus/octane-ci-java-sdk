package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;

import java.util.List;
import java.util.Map;


public class CucumberJVMConverter extends TestsToRunConverter {

    private static final String FEATURE_FILE_PATH = "featureFilePath";

    @Override
    protected String convert(List<TestToRunData> data, String executionDirectory, Map<String, String> globalParameters) {


        StringBuilder sb = new StringBuilder();
        String classJoiner = "";

        for (TestToRunData testData : data) {
            String featureFilePath = getFeatureFilePath(testData);
            if (featureFilePath != null && !featureFilePath.isEmpty()) {
                sb.append(classJoiner);
                sb.append("'");
                sb.append(featureFilePath);
                sb.append("'");
                classJoiner = " ";
            }

        }
        return sb.toString();

    }

    private String getFeatureFilePath(TestToRunData item) {
        return item.getParameter(FEATURE_FILE_PATH);
    }
}
