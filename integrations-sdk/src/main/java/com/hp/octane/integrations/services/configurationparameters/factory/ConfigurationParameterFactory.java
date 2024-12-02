/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hp.octane.integrations.services.configurationparameters.factory;

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.services.configurationparameters.*;

import java.util.NoSuchElementException;

public class ConfigurationParameterFactory {

	public static ConfigurationParameter tryCreate(String paramKey, String paramValue) {
		switch (paramKey) {
			case EncodeCiJobBase64Parameter.KEY:
				return EncodeCiJobBase64Parameter.create(paramValue);
			case UftTestRunnerFolderParameter.KEY:
				return UftTestRunnerFolderParameter.create(paramValue);
			case UftTestConnectionDisabledParameter.KEY:
				return UftTestConnectionDisabledParameter.create(paramValue);
			case SCMRestAPIParameter.KEY:
				return SCMRestAPIParameter.create(paramValue);
			case FortifySSCTokenParameter.KEY:
				return FortifySSCTokenParameter.create(paramValue);
			case FortifySSCFetchTimeoutParameter.KEY:
				return FortifySSCFetchTimeoutParameter.create(paramValue);
			case SendEventsInBulkParameter.KEY:
				return SendEventsInBulkParameter.create(paramValue);
			case UftTestsDeepRenameParameter.KEY:
				return UftTestsDeepRenameParameter.create(paramValue);
			case LogEventsParameter.KEY:
				return LogEventsParameter.create(paramValue);
			case JobListCacheAllowedParameter.KEY:
				return JobListCacheAllowedParameter.create(paramValue);
			case OctaneRootsCacheAllowedParameter.KEY:
				return OctaneRootsCacheAllowedParameter.create(paramValue);
			case AddGlobalParameterToTestsParameter.KEY:
				return AddGlobalParameterToTestsParameter.create(paramValue);
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
		return EncodeCiJobBase64Parameter.DEFAULT;
	}

	public static boolean isSCMRestAPI(OctaneConfiguration configuration) {
		SCMRestAPIParameter scmRestAPIParameter = (SCMRestAPIParameter) configuration.getParameter(SCMRestAPIParameter.KEY);
		if (scmRestAPIParameter != null) {
			return scmRestAPIParameter.isSCMRestAPI();
		}
		return SCMRestAPIParameter.DEFAULT;
	}

	public static boolean isSendEventsInBulk(OctaneConfiguration configuration) {
		SendEventsInBulkParameter param = (SendEventsInBulkParameter) configuration.getParameter(SendEventsInBulkParameter.KEY);
		if (param != null) {
			return param.isBulk();
		}
		return SendEventsInBulkParameter.DEFAULT;
	}

	public static boolean isUftTestsDeepRenameCheckEnabled(OctaneConfiguration configuration) {
		UftTestsDeepRenameParameter param = (UftTestsDeepRenameParameter) configuration.getParameter(UftTestsDeepRenameParameter.KEY);
		if (param != null) {
			return param.isUftTestsDeepRenameCheckEnabled();
		}
		return UftTestsDeepRenameParameter.DEFAULT;
	}



	public static boolean isLogEvents(OctaneConfiguration configuration) {
		LogEventsParameter param = (LogEventsParameter) configuration.getParameter(LogEventsParameter.KEY);
		if (param != null) {
			return param.isLogEvents();
		}
		return LogEventsParameter.DEFAULT;
	}

	public static boolean isUftTestConnectionDisabled(OctaneConfiguration configuration) {
		UftTestConnectionDisabledParameter param = (UftTestConnectionDisabledParameter) configuration.getParameter(UftTestConnectionDisabledParameter.KEY);
		if (param != null) {
			return param.isDisabled();
		}
		return UftTestConnectionDisabledParameter.DEFAULT;
	}

	public static boolean jobListCacheAllowed(OctaneConfiguration configuration) {
		JobListCacheAllowedParameter param = (JobListCacheAllowedParameter) configuration.getParameter(JobListCacheAllowedParameter.KEY);
		if (param != null) {
			return param.isAllowed();
		}
		return JobListCacheAllowedParameter.DEFAULT;
	}

	public static boolean octaneRootsCacheAllowed(OctaneConfiguration configuration) {
		OctaneRootsCacheAllowedParameter param = (OctaneRootsCacheAllowedParameter) configuration.getParameter(OctaneRootsCacheAllowedParameter.KEY);
		if (param != null) {
			return param.isAllowed();
		}
		return OctaneRootsCacheAllowedParameter.DEFAULT;
	}

	public static boolean addGlobalParametersToTests(OctaneConfiguration configuration) {
		AddGlobalParameterToTestsParameter param = (AddGlobalParameterToTestsParameter) configuration.getParameter(AddGlobalParameterToTestsParameter.KEY);
		if (param != null) {
			return param.isToAdd();
		}
		return AddGlobalParameterToTestsParameter.DEFAULT;
	}


	public static Boolean validateBooleanValue(String rawValue, String key){
		if (rawValue == null) {
			throw new IllegalArgumentException("Parameter " + key + " : Expected boolean value (true/false)");
		}

		if (!(rawValue.equalsIgnoreCase("true") || rawValue.equalsIgnoreCase("false"))) {
			throw new IllegalArgumentException("Parameter " + key + " : Expected boolean value (true/false)");
		}

		return Boolean.parseBoolean(rawValue);
	}
}
