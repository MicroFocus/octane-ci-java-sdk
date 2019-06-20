/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.CIPluginSDKInfo;
import com.hp.octane.integrations.dto.general.CIProviderSummaryInfo;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;

/**
 * Description of Plugin Status
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class CIProviderSummaryInfoImpl implements CIProviderSummaryInfo {
	private CIServerInfo server;
	private CIPluginInfo plugin;
	private CIPluginSDKInfo sdk;

	@Override
	public CIServerInfo getServer() {
		return server;
	}

	@Override
	public CIProviderSummaryInfo setServer(CIServerInfo server) {
		this.server = server;
		return this;
	}

	@Override
	public CIPluginInfo getPlugin() {
		return plugin;
	}

	@Override
	public CIProviderSummaryInfo setPlugin(CIPluginInfo plugin) {
		this.plugin = plugin;
		return this;
	}

	@Override
	public CIPluginSDKInfo getSdk() {
		return sdk;
	}

	@Override
	public CIProviderSummaryInfo setSdk(CIPluginSDKInfo sdk) {
		this.sdk = sdk;
		return this;
	}
}
