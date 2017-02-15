/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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

package com.hp.octane.integrations.dto.pipelines.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.pipelines.BuildHistory;
import com.hp.octane.integrations.dto.scm.SCMData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: sadea
 * Date: 29/07/15
 * Time: 14:53
 * To change this template use File | Settings | File Templates.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class BuildHistoryImpl implements BuildHistory {
	private List<Build> builds = new ArrayList<>();
	private Build lastSuccesfullBuild;
	private Build lastBuild;

	public void addBuild(String status, String number, String time, String startTime, String duration, SCMData scmData, Set<SCMUser> culprits) {
		builds.add(new Build(status, number, time, startTime, duration, scmData, culprits));
	}

	public void addLastSuccesfullBuild(String status, String number, String time, String startTime, String duration, SCMData scmData, Set<SCMUser> culprits) {
		lastSuccesfullBuild = new Build(status, number, time, startTime, duration, scmData, culprits);
	}

	public void addLastBuild(String status, String number, String time, String startTime, String duration, SCMData scmData, Set<SCMUser> culprits) {
		lastBuild = new Build(status, number, time, startTime, duration, scmData, culprits);
	}

	public Build getLastSuccesfullBuild() {
		return lastSuccesfullBuild;
	}

	public Build[] getBuilds() {
		return builds.toArray(new Build[builds.size()]);
	}

	public Build getLastBuild() {
		return lastBuild;
	}
}
