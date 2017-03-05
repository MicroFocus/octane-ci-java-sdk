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

package com.hp.octane.integrations.dto.events;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.scm.SCMData;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;

import java.util.List;

/**
 * CI Event DTO
 */

public interface CIEvent extends DTOBase {

	String getProjectDisplayName();

	CIEvent setProjectDisplayName(String projectDisplayName);

	CIEventType getEventType();

	CIEvent setEventType(CIEventType type);

	String getBuildCiId();

	CIEvent setPhaseType(PhaseType phaseType);

	CIEvent setBuildCiId(String buildCiId);

	String getProject();

	CIEvent setProject(String project);

	String getNumber();

	CIEvent setNumber(String number);

	List<CIEventCause> getCauses();

	CIEvent setCauses(List<CIEventCause> causes);

	List<CIParameter> getParameters();

	CIEvent setParameters(List<CIParameter> parameters);

	CIBuildResult getResult();

	CIEvent setResult(CIBuildResult result);

	Long getStartTime();

	CIEvent setStartTime(Long startTime);

	Long getEstimatedDuration();

	CIEvent setEstimatedDuration(Long estimatedDuration);

	Long getDuration();

	CIEvent setDuration(Long duration);

	SCMData getScmData();

	CIEvent setScmData(SCMData scmData);
}
