package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.hp.octane.integrations.utils.SdkStringUtils;

import java.util.List;
import java.util.stream.Collectors;

/*
 * Converter to any given format
 */
public class CustomConverter extends TestsToRunConverter {

    private static final String $_PACKAGE = "$package";
    private static final String $_CLASS = "$class";
    private static final String $_TEST_NAME = "$testName";

    protected String format;
    protected String delimiter;

    public CustomConverter(String format, String delimiter) {
        this.format = format;
        this.delimiter = delimiter;
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
        int packageIndex = format.indexOf($_PACKAGE);

        String res = format;

        if (formatContainsPackage){
            String packageName = testToRunData.getPackageName();
            if (SdkStringUtils.isNotEmpty(packageName)) {
                res = res.replace($_PACKAGE, packageName);
            } else {
                // remove $package part of format including its delimiter
                if (formatContainsClass) {
                    // the $class expresion exists in given format - remove the part till $class
                    //      for example: the format is XXXX$package.||.$class.||.$testName
                    //      the result: XXXX$class.||.$testName
                    res = splice(res, packageIndex, res.indexOf($_CLASS));
                } else {
                    // no $class expresion exists in given format - remove the part till $testName
                    //      for example: the format is XXXX$package.||.$testName
                    //      the result: XXXX$testName
                    res = splice(res, packageIndex, res.indexOf($_TEST_NAME));
                }
            }
        }

        if (formatContainsClass){
            String className = testToRunData.getClassName();
            if (SdkStringUtils.isNotEmpty(className)) {
                res = res.replace($_CLASS, className);
            } else {
                // remove $class part of format including its delimiter (till $testName)
                //      for example: the format is XXXX$class.||.$testName
                //      the result: XXXX$testName
                res = splice(res, res.indexOf($_CLASS), res.indexOf($_TEST_NAME));
            }
        }

        res = res.replace($_TEST_NAME, testToRunData.getTestName());

        return res;
    }


    /**
     * method changes the contents of a string by removing existing elements form index to index
     *
     * @param string the original string
     * @param beginIndex the begin index to remove the characters
     * @param endIndex the begin index to remove the characters
     * @return a new string contains the a part of the given string without existing substring form begin to end
     *
      */
    private String splice(String string, int beginIndex, int endIndex) {
        return string.substring(0, beginIndex) + string.substring(endIndex);
    }

}
