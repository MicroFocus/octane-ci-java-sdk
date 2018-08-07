package com.hp.octane.integrations.executor;

import com.hp.octane.integrations.executor.converters.JUnit4MavenConverter;
import com.hp.octane.integrations.executor.converters.MfUftConverter;


public class TestsToRunConvertersFactory {

    public static TestsToRunConverter createConverter(TestsToRunFramework framework){
        switch (framework){
            case JUnit4:
                return new JUnit4MavenConverter();
            case MF_UFT:
                return new MfUftConverter();
                default:
                    throw new UnsupportedOperationException(framework.name() + " framework does not have supported converter");
        }

    }
}
