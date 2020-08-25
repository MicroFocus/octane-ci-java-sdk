package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;

public class SendEventsInBulk implements ConfigurationParameter {
	public static final String KEY = "SEND_EVENTS_IN_BULK";
	private boolean isBulk;
	public static final boolean DEFAULT = true;

	private SendEventsInBulk(boolean isBulk) {
		this.isBulk = isBulk;
	}

	public boolean isBulk() {
		return isBulk;
	}

	public static SendEventsInBulk create(String rawValue) {
		if (rawValue == null) {
			throw new IllegalArgumentException("Parameter " + KEY + " : Expected boolean value (true/false)");
		}

		if (!(rawValue.equalsIgnoreCase("true") || rawValue.equalsIgnoreCase("false"))) {
			throw new IllegalArgumentException("Parameter " + KEY + " : Expected boolean value (true/false)");
		}

		return new SendEventsInBulk(Boolean.parseBoolean(rawValue));
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getRawValue() {
		return Boolean.toString(isBulk);
	}
}
