package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.hp.octane.integrations.utils.SdkStringUtils;

import java.util.List;
import java.util.stream.Collectors;

/*
 * Converter to gradle format : gradle test --tests integTest1 --tests integTest12
 */
public class GradleConverter extends CustomConverter {

    public static final String GRADLE_FORMAT = " --tests $package.$class.$testName";
    public static final String GRADLE_DELIMITER = "";

    public GradleConverter() {
        super(GRADLE_FORMAT, GRADLE_DELIMITER);
    }

}
