package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;

import java.util.List;


public class JBehaveJVMConverter extends CustomConverter {

    public static final String FORMAT = "{\"testPattern\": \"$featureFilePath\",\"testDelimiter\": \",\"}";

    public JBehaveJVMConverter() {
        super(FORMAT);
    }


}