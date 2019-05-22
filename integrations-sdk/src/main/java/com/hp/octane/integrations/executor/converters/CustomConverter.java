package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.hp.octane.integrations.utils.SdkStringUtils;

import java.util.List;
import java.util.stream.Collectors;

/*
 * Converter custom format : //fromat="$package.$class#$testName" delimiter="|"
 * mvn clean -Dtest=MF.simple.tests.App2Test#testSendGet,MF.simple.tests.AppTest#testAlwaysFail test
 */
public class CustomConverter extends TestsToRunConverter {

    private static final String $_PACKAGE = "$package";
    private static final String $_CLASS = "$class";
    private static final String $_TEST_NAME = "$testName";

    public CustomConverter(String format, String delimiter) {
        super(format, delimiter);
    }

    @Override
    public String convert(List<TestToRunData> data, String executionDirectory) {

        String collect = data.stream()
                .map(n -> convertToFormat(n) )
                .collect(Collectors.joining(delimiter));
        return collect;
    }

    private String convertToFormat(TestToRunData testToRunData) {

        boolean formatContainsPackage = format.contains($_PACKAGE);
        boolean formatContainsClass = format.contains($_CLASS);
        int indPackage = format.indexOf($_PACKAGE);
        int indClass = format.indexOf($_CLASS);

        String res = format;

        if (formatContainsPackage){
            String packageName = testToRunData.getPackageName();
            if (SdkStringUtils.isNotEmpty(packageName)) {
                res = format.replace($_PACKAGE, packageName);
            } else {
                if (formatContainsClass) {
                    res = res.substring(0, indPackage - 1) + res.substring(indClass);
                } else {
                    res = res.substring(0, indPackage) + res.substring(format.indexOf($_TEST_NAME));
                }
            }
        }

        if (formatContainsClass){
            String className = testToRunData.getClassName();
            if (SdkStringUtils.isNotEmpty(className)) {
                res = res.replace($_CLASS, className);
            } else {
                res = res.substring(0, indClass) + res.substring(res.indexOf($_TEST_NAME));

            }
        }

        res = res.replace($_TEST_NAME, testToRunData.getTestName());

        return res;
    }

}
