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

package com.hp.octane.integrations.dto.snapshots;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.scm.SCMData;

import java.util.List;

/**
 * SnapshotNode DTO
 */

public interface SnapshotNode extends DTOBase {

	String getJobCiId();

	SnapshotNode setJobCiId(String jobCiId);

	String getName();

	SnapshotNode setName(String name);

	String getBuildCiId();

	SnapshotNode setBuildCiId(String buildCiId);

	String getNumber();

	SnapshotNode setNumber(String number);

	List<CIEventCause> getCauses();

	SnapshotNode setCauses(List<CIEventCause> causes);

	CIBuildStatus getStatus();

	SnapshotNode setStatus(CIBuildStatus status);

	CIBuildResult getResult();

	SnapshotNode setResult(CIBuildResult result);

	Long getEstimatedDuration();

	SnapshotNode setEstimatedDuration(Long estimatedDuration);

	Long getStartTime();

	SnapshotNode setStartTime(Long startTime);

	Long getDuration();

	SnapshotNode setDuration(Long duration);

	SCMData getScmData();

	SnapshotNode setScmData(SCMData scmData);

	List<CIParameter> getParameters();

	SnapshotNode setParameters(List<CIParameter> parameters);

	List<SnapshotPhase> getPhasesInternal();

	SnapshotNode setPhasesInternal(List<SnapshotPhase> phasesInternal);

	List<SnapshotPhase> getPhasesPostBuild();

	SnapshotNode setPhasesPostBuild(List<SnapshotPhase> phasesPostBuild);
}
