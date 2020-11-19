package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;

import java.util.List;


public class JBehaveConverter extends CustomConverter {

    public static final String FORMAT = "{\"testPattern\": \"$featureFilePath\",\"testDelimiter\": \",\"}";

    public JBehaveConverter() {
        super(FORMAT);
    }


}