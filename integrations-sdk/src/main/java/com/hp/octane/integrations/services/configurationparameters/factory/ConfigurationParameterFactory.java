package com.hp.octane.integrations.services.configurationparameters.factory;

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.services.configurationparameters.EncodeCiJobBase64Parameter;
import com.hp.octane.integrations.services.configurationparameters.SCMRestAPIParameter;
import com.hp.octane.integrations.services.configurationparameters.UftTestRunnerFolderParameter;

import java.util.NoSuchElementException;

public class ConfigurationParameterFactory {

	public static ConfigurationParameter tryCreate(String paramKey, String paramValue) {
		switch (paramKey) {
			case EncodeCiJobBase64Parameter.KEY:
				return EncodeCiJobBase64Parameter.create(paramValue);
			case UftTestRunnerFolderParameter.KEY:
				return UftTestRunnerFolderParameter.create(paramValue);
			case SCMRestAPIParameter.KEY:
				return SCMRestAPIParameter.create(paramValue);
			default:
				throw new NoSuchElementException("Unknown parameter : " + paramKey);
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

	public static boolean isSCMRestAPI(OctaneConfiguration configuration) {
		SCMRestAPIParameter scmRestAPIParameter = (SCMRestAPIParameter) configuration.getParameter(SCMRestAPIParameter.KEY);
		if (scmRestAPIParameter != null) {
			return scmRestAPIParameter.isSCMRestAPI();
		}
		return true;
	}

}
