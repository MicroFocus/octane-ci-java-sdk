package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;

/*
Whether  log sent events to log or not
 */
public class LogEventsParameter implements ConfigurationParameter {
	public static final String KEY = "LOG_EVENTS";
	private boolean logEvents;
	public static final boolean DEFAULT = false;

	private LogEventsParameter(boolean logEvents) {
		this.logEvents = logEvents;
	}

	public boolean isLogEvents() {
		return logEvents;
	}

	public static LogEventsParameter create(String rawValue) {
		if (rawValue == null) {
			throw new IllegalArgumentException("Parameter " + KEY + " : Expected boolean value (true/false)");
		}

		if (!(rawValue.equalsIgnoreCase("true") || rawValue.equalsIgnoreCase("false"))) {
			throw new IllegalArgumentException("Parameter " + KEY + " : Expected boolean value (true/false)");
		}

		return new LogEventsParameter(Boolean.parseBoolean(rawValue));
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getRawValue() {
		return Boolean.toString(logEvents);
	}
}
