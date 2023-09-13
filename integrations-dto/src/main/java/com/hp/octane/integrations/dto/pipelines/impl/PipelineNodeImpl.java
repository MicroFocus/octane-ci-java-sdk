/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
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

package com.hp.octane.integrations.dto.pipelines.impl;

import com.hp.octane.integrations.dto.events.MultiBranchType;
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
	private MultiBranchType multiBranchType;
	private List<CIParameter> parameters = new ArrayList<>();
	private List<PipelinePhase> phasesInternal = new ArrayList<>();
	private List<PipelinePhase> phasesPostBuild = new ArrayList<>();
	private Boolean hasUpstream;
	private Boolean isTestRunner;
	private String defaultBranchName;

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

	@Override
	public MultiBranchType getMultiBranchType() {
		return multiBranchType;
	}

	@Override
	public PipelineNode setMultiBranchType(MultiBranchType multiBranchType) {
		this.multiBranchType = multiBranchType;
		return this;
	}

	@Override
	public Boolean getHasUpstream() {
		return hasUpstream;
	}

	@Override
	public PipelineNode setHasUpstream(Boolean hasUpstream) {
		this.hasUpstream = hasUpstream;
		return this;
	}

	@Override
	public Boolean getIsTestRunner() {
		return isTestRunner;
	}

	@Override
	public PipelineNode setIsTestRunner(Boolean isTestRunner) {
		this.isTestRunner = isTestRunner;
		return this;
	}

	@Override
	public String getDefaultBranchName() {
		return defaultBranchName;
	}

	@Override
	public PipelineNode setDefaultBranchName(String defaultBranchName) {
		this.defaultBranchName = defaultBranchName;
		return this;
	}
}
