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
package com.hp.octane.integrations.dto.events.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.events.*;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.scm.SCMData;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;

import java.util.List;
import java.util.Map;

/**
 * Base implementation of CI Event object
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class CIEventImpl implements CIEvent {
	private CIEventType eventType;
	private String buildCiId;
	private String project;
	private String parentCiId;
	private MultiBranchType multiBranchType;
	private String number;
	private List<CIEventCause> causes;
	private List<CIParameter> parameters;
	private CIBuildResult result;
	private Long startTime;
	private Long estimatedDuration;
	private Long duration;
	private SCMData scmData;
	private Boolean testResultExpected;
	private String projectDisplayName;
	private PhaseType phaseType;
	private String commonHashId;
	private String branchName;
    private Map<String, String> environmentOutputtedParameters;
	private String previousProject;
	private String previousProjectDisplayName;
	private ItemType itemType;
	private Boolean skipValidation;
	private Boolean isVirtualProject;
	private String stateLogicalName;

	public PhaseType getPhaseType() {
		return phaseType;
	}

	public CIEvent setPhaseType(PhaseType phaseType) {
		this.phaseType = phaseType;
		return this;
	}


	public String getProjectDisplayName() {
		return projectDisplayName;
	}

	public CIEvent setProjectDisplayName(String projectDisplayName) {
		this.projectDisplayName = projectDisplayName;
		return this;
	}

	public Boolean getSkipValidation() {
		return skipValidation;
	}

	public CIEvent setSkipValidation(Boolean skipValidation) {
		this.skipValidation = skipValidation;
		return this;
	}

	@Override
	public Boolean getIsVirtualProject() {
		return isVirtualProject;
	}

	@Override
	public CIEvent setIsVirtualProject(Boolean isVirtualJob) {
		this.isVirtualProject = isVirtualJob;
		return this;
	}

	public CIEventType getEventType() {
		return eventType;
	}

	public CIEvent setEventType(CIEventType eventType) {
		this.eventType = eventType;
		return this;
	}

	public String getBuildCiId() {
		return buildCiId;
	}

	public CIEvent setBuildCiId(String buildCiId) {
		this.buildCiId = buildCiId;
		return this;
	}

	public String getProject() {
		return project;
	}

	public CIEvent setProject(String project) {
		this.project = project;
		return this;
	}

	@Override
	public String getParentCiId() {
		return this.parentCiId;
	}

	@Override
	public CIEvent setParentCiId(String parentCiId) {
		this.parentCiId = parentCiId;
		return this;
	}

	@Override
	public MultiBranchType getMultiBranchType() {
		return multiBranchType;
	}

	@Override
	public CIEvent setMultiBranchType(MultiBranchType multiBranchType) {
		this.multiBranchType = multiBranchType;
		return this;
	}

	@Override
	public ItemType getItemType() {
		return itemType;
	}

	@Override
	public CIEvent setItemType(ItemType itemType) {
		this.itemType = itemType;
		return this;
	}

	@Override
	public CIEvent setPreviousProject(String previousProject) {
		this.previousProject = previousProject;
		return this;
	}

	@Override
	public CIEvent setPreviousProjectDisplayName(String previousProjectDisplayName) {
		this.previousProjectDisplayName = previousProjectDisplayName;
		return this;
	}

	@Override
	public String getPreviousProject() {
		return previousProject;
	}

	@Override
	public String getPreviousProjectDisplayName() {
		return previousProjectDisplayName;
	}

	public String getNumber() {
		return number;
	}

	public CIEvent setNumber(String number) {
		this.number = number;
		return this;
	}

	public List<CIEventCause> getCauses() {
		return causes;
	}

	public CIEvent setCauses(List<CIEventCause> causes) {
		this.causes = causes;
		return this;
	}

	public List<CIParameter> getParameters() {
		return parameters;
	}

	public CIEvent setParameters(List<CIParameter> parameters) {
		this.parameters = parameters;
		return this;
	}

	public CIBuildResult getResult() {
		return result;
	}

	public CIEvent setResult(CIBuildResult result) {
		this.result = result;
		return this;
	}

	public Long getStartTime() {
		return startTime;
	}

	public CIEvent setStartTime(Long startTime) {
		this.startTime = startTime;
		return this;
	}

	public Long getEstimatedDuration() {
		return estimatedDuration;
	}

	public CIEvent setEstimatedDuration(Long estimatedDuration) {
		this.estimatedDuration = estimatedDuration;
		return this;
	}

	public Long getDuration() {
		return duration;
	}

	public CIEvent setDuration(Long duration) {
		this.duration = duration;
		return this;
	}

	public SCMData getScmData() {
		return scmData;
	}

	public CIEvent setScmData(SCMData scmData) {
		this.scmData = scmData;
		return this;
	}

	public String getCommonHashId() {
		return commonHashId;
	}

	public CIEvent setCommonHashId(String commonHashId) {
		this.commonHashId = commonHashId;
		return this;
	}



	public String getBranchName() {
		return branchName;
	}

	public CIEvent setBranchName(String branchName) {
		this.branchName = branchName;
		return this;
	}

	@Override
	public Map<String, String> getEnvironmentOutputtedParameters() {
		return environmentOutputtedParameters;
	}

	@Override
	public CIEvent setEnvironmentOutputtedParameters(Map<String, String> environmentOutputtedParameters) {
		this.environmentOutputtedParameters = environmentOutputtedParameters;
		return this;
	}

	@Override
	public Boolean getTestResultExpected() {
		return testResultExpected;
	}

	@Override
	public CIEvent setTestResultExpected(boolean expected) {
		this.testResultExpected = expected;
		return this;
	}

	public String getStateLogicalName() {
		return stateLogicalName;
	}

	public CIEvent setStateLogicalName(String stateLogicalName) {
		this.stateLogicalName = stateLogicalName;
		return this;
	}
}
