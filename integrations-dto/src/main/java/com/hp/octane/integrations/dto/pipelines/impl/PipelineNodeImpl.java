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

import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.pipelines.PipelinePhase;

import java.util.ArrayList;
import java.util.List;

/**
 * PipelineNode DTO implementation.
 */

class PipelineNodeImpl implements PipelineNode {
	private String jobCiId;
	private String name;
	private List<CIParameter> parameters = new ArrayList<>();
	private List<PipelinePhase> phasesInternal = new ArrayList<>();
	private List<PipelinePhase> phasesPostBuild = new ArrayList<>();

	public String getJobCiId() {
		return jobCiId;
	}

	public PipelineNode setJobCiId(String jobCiId) {
		this.jobCiId = jobCiId;
		return this;
	}

	public String getName() {
		return name;
	}

	public PipelineNode setName(String name) {
		this.name = name;
		return this;
	}

	public List<CIParameter> getParameters() {
		return parameters;
	}

	public PipelineNode setParameters(List<CIParameter> parameters) {
		this.parameters = parameters;
		return this;
	}

	public List<PipelinePhase> getPhasesInternal() {
		return phasesInternal;
	}

	public PipelineNode setPhasesInternal(List<PipelinePhase> phasesInternal) {
		this.phasesInternal = phasesInternal;
		return this;
	}

	public List<PipelinePhase> getPhasesPostBuild() {
		return phasesPostBuild;
	}

	public PipelineNode setPhasesPostBuild(List<PipelinePhase> phasesPostBuild) {
		this.phasesPostBuild = phasesPostBuild;
		return this;
	}
}
