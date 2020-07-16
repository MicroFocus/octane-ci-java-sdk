package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;

public class FortifySSCMaxTimeoutHours implements ConfigurationParameter {
    public static final String KEY = "FORTIFY_SSC_MAX_TIMEOUT_HOURS";
    public static final int DEFAULT_TIMEOUT = 12;
    private int timeout;

    private FortifySSCMaxTimeoutHours(int timeout) {
        this.timeout = timeout;
    }


    public static FortifySSCMaxTimeoutHours create(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter " + KEY + " : Expected non-empty integer value");
        }

        int value;
        try {
            value = Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter " + KEY + " : Expected integer value");
        }

        if (value <= 0 || value > 12) {
            throw new IllegalArgumentException("Parameter " + KEY + " : Expected integer value. The valid values are 1-12.");
        }

        return new FortifySSCMaxTimeoutHours(value);
    }

    @Override
    public String getKey() {
        return KEY;
    }

    public int getTimeout() {
        return timeout;
    }

    @Override
    public String getRawValue() {
        return Integer.toString(timeout);
    }
}
