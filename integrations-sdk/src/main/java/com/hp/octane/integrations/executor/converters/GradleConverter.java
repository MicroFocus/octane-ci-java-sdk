package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.hp.octane.integrations.utils.SdkStringUtils;

import java.util.List;
import java.util.stream.Collectors;

/*
 * Converter to gradle format : gradle test --tests integTest1 --tests integTest12
 */
public class GradleConverter extends TestsToRunConverter {

    public GradleConverter(String format, String delimiter) {
        super(format, delimiter);
    }

    @Override
    public String convert(List<TestToRunData> data, String executionDirectory) {
        return data.stream()
                .map( n -> " --tests " + getTestFullPath(n))
                .collect( Collectors.joining( "" ) );
    }

    private String getTestFullPath(TestToRunData testToRunData) {
        String res = "";
        if (SdkStringUtils.isNotEmpty(testToRunData.getPackageName())) {
            res = testToRunData.getPackageName() + ".";
        }
        res += testToRunData.getClassName() + "." + testToRunData.getTestName();

        return res;
    }

}
