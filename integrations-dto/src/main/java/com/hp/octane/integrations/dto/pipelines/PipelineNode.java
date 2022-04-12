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

package com.hp.octane.integrations.dto.pipelines;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.events.MultiBranchType;
import com.hp.octane.integrations.dto.parameters.CIParameter;

import java.util.List;


/**
 * Pipeline node DTO
 */
public interface PipelineNode extends DTOBase {

	String getJobCiId();

	PipelineNode setJobCiId(String jobCiId);

	String getName();

	PipelineNode setName(String name);

	List<CIParameter> getParameters();

	PipelineNode setParameters(List<CIParameter> parameters);

	List<PipelinePhase> getPhasesInternal();

	PipelineNode setPhasesInternal(List<PipelinePhase> phasesInternal);

	List<PipelinePhase> getPhasesPostBuild();

	PipelineNode setPhasesPostBuild(List<PipelinePhase> phasesPostBuild);

	MultiBranchType getMultiBranchType();

	PipelineNode setMultiBranchType(MultiBranchType multiBranchType);

	Boolean getHasUpstream();

	PipelineNode setHasUpstream(Boolean hasUpstream);

	Boolean getIsTestRunner();

	PipelineNode setIsTestRunner(Boolean isTestRunner);

	String getDefaultBranchId();

	PipelineNode setDefaultBranchId(String defaultBranchId);
}
