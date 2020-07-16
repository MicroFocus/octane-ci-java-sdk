package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;

public class FortifySSCTokenParameter implements ConfigurationParameter {
    public static final String KEY = "FORTIFY_SSC_TOKEN";
    private String token;

    private FortifySSCTokenParameter(String token) {
        this.token = token;
    }


    public static FortifySSCTokenParameter create(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter " + KEY + " : Expected non-empty string value");
        }

        return new FortifySSCTokenParameter(rawValue);
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getRawValue() {
        return this.token.substring(0, 1) + "******";
    }

    public String getToken(){
        return this.token;
    }
}
