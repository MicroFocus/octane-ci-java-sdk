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

package com.hp.octane.integrations.dto.snapshots.impl;

import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.scm.SCMData;
import com.hp.octane.integrations.dto.snapshots.SnapshotNode;
import com.hp.octane.integrations.dto.snapshots.SnapshotPhase;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.hp.octane.integrations.dto.snapshots.CIBuildStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * SnapshotNode DTO implementation.
 */

class SnapshotNodeImpl implements SnapshotNode {
	private String jobCiId;
	private String name;
	private String buildCiId;
	private String number;
	private List<CIEventCause> causes = new ArrayList<>();
	private CIBuildStatus status = CIBuildStatus.UNAVAILABLE;
	private CIBuildResult result = CIBuildResult.UNAVAILABLE;
	private Long estimatedDuration;
	private Long startTime;
	private Long duration;
	private SCMData scmData;
	private List<CIParameter> parameters = new ArrayList<>();
	private List<SnapshotPhase> phasesInternal = new ArrayList<>();
	private List<SnapshotPhase> phasesPostBuild = new ArrayList<>();

	public String getJobCiId() {
		return jobCiId;
	}

	public SnapshotNode setJobCiId(String jobCiId) {
		this.jobCiId = jobCiId;
		return this;
	}

	public String getName() {
		return name;
	}

	public SnapshotNode setName(String name) {
		this.name = name;
		return this;
	}

	public String getBuildCiId() {
		return buildCiId;
	}

	public SnapshotNode setBuildCiId(String buildCiId) {
		this.buildCiId = buildCiId;
		return this;
	}

	public String getNumber() {
		return number;
	}

	public SnapshotNode setNumber(String number) {
		this.number = number;
		return this;
	}

	public List<CIEventCause> getCauses() {
		return causes;
	}

	public SnapshotNode setCauses(List<CIEventCause> causes) {
		this.causes = causes;
		return this;
	}

	public CIBuildStatus getStatus() {
		return status;
	}

	public SnapshotNode setStatus(CIBuildStatus status) {
		this.status = status;
		return this;
	}

	public CIBuildResult getResult() {
		return result;
	}

	public SnapshotNode setResult(CIBuildResult result) {
		this.result = result;
		return this;
	}

	public Long getEstimatedDuration() {
		return estimatedDuration;
	}

	public SnapshotNode setEstimatedDuration(Long estimatedDuration) {
		this.estimatedDuration = estimatedDuration;
		return this;
	}

	public Long getStartTime() {
		return startTime;
	}

	public SnapshotNode setStartTime(Long startTime) {
		this.startTime = startTime;
		return this;
	}

	public Long getDuration() {
		return duration;
	}

	public SnapshotNode setDuration(Long duration) {
		this.duration = duration;
		return this;
	}

	public SCMData getScmData() {
		return scmData;
	}

	public SnapshotNode setScmData(SCMData scmData) {
		this.scmData = scmData;
		return this;
	}

	public List<CIParameter> getParameters() {
		return parameters;
	}

	public SnapshotNode setParameters(List<CIParameter> parameters) {
		this.parameters = parameters;
		return this;
	}

	public List<SnapshotPhase> getPhasesInternal() {
		return phasesInternal;
	}

	public SnapshotNode setPhasesInternal(List<SnapshotPhase> phasesInternal) {
		this.phasesInternal = phasesInternal;
		return this;
	}

	public List<SnapshotPhase> getPhasesPostBuild() {
		return phasesPostBuild;
	}

	public SnapshotNode setPhasesPostBuild(List<SnapshotPhase> phasesPostBuild) {
		this.phasesPostBuild = phasesPostBuild;
		return this;
	}
}
