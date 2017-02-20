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

package com.hp.octane.integrations.dto.pipelines;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.scm.SCMData;

import java.util.Set;

/**
 * BuildHistory DTO
 */

@Deprecated
public interface BuildHistory extends DTOBase {

	class Build {
		private String status;
		private String number;
		private String time;
		private String startTime;
		private String duration;
		private SCMData scmData;
		private Set<SCMUser> culprits;


		Build(String status, String number, String time) {
			this.status = status;
			this.number = number;
			this.time = time;
		}

		public Build(String status, String number, String time, String startTime, String duration, SCMData scmData, Set<SCMUser> culprits) {
			this.status = status;
			this.number = number;
			this.time = time;
			this.startTime = startTime;
			this.duration = duration;
			this.scmData = scmData;
			this.culprits = culprits;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public String getTime() {
			return time;
		}

		public void setTime(String time) {
			this.time = time;
		}

		public String getStartTime() {
			return startTime;
		}

		public String getDuration() {
			return duration;
		}

		public SCMData getScmData() {
			return scmData;
		}

		public Set<SCMUser> getCulprits() {
			return culprits;
		}
	}

	void addBuild(String status, String number, String time, String startTime, String duration, SCMData scmData, Set<SCMUser> culprits);

	void addLastSuccesfullBuild(String status, String number, String time, String startTime, String duration, SCMData scmData, Set<SCMUser> culprits);

	void addLastBuild(String status, String number, String time, String startTime, String duration, SCMData scmData, Set<SCMUser> culprits);

	Build getLastSuccesfullBuild();

	Build[] getBuilds();

	Build getLastBuild();

	class SCMUser {
		private String id;
		private String fullName;
		private String displayName;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getFullName() {
			return fullName;
		}

		public void setFullName(String fullName) {
			this.fullName = fullName;
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}
	}
}
