package com.hp.octane.integrations.services.configurationparameters.factory;

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.services.configurationparameters.EncodeCiJobBase64Parameter;

public class ConfigurationParameterFactory {

	public static ConfigurationParameter tryCreate(String paramKey, String paramValue) {
		switch (paramKey) {
			case EncodeCiJobBase64Parameter.KEY:
				return EncodeCiJobBase64Parameter.create(paramValue);
			default:
				throw new IllegalArgumentException("Unknown parameter : " + paramKey);
		}
	}

	public static void addParameter(OctaneConfiguration conf, String paramKey, String paramValue) {
		try {
			ConfigurationParameter param = tryCreate(paramKey, paramValue);
			conf.addParameter(param);
		} catch (Exception e) {
			//do nothing
		}
	}

	public static boolean isEncodeCiJobBase64(OctaneConfiguration configuration) {
		EncodeCiJobBase64Parameter encodeCiJobBase64 = (EncodeCiJobBase64Parameter) configuration.getParameter(EncodeCiJobBase64Parameter.KEY);
		if (encodeCiJobBase64 != null) {
			return encodeCiJobBase64.isEncoded();
		}
		return false;
	}
}
