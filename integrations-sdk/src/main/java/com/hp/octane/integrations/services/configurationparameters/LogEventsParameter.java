package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;

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
		return new LogEventsParameter(ConfigurationParameterFactory.validateBooleanValue(rawValue,KEY));
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
