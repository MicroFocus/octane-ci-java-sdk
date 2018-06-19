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

package com.hp.octane.integrations.dto.pipelines.impl;

import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.pipelines.PipelinePhase;

import java.util.ArrayList;
import java.util.List;

/**
 * PipelinePhase DTO implementation.
 */

class PipelinePhaseImpl implements PipelinePhase {
	private String name;
	private boolean blocking;
	private List<PipelineNode> jobs = new ArrayList<>();

	public String getName() {
		return name;
	}

	public PipelinePhase setName(String name) {
		this.name = name;
		return this;
	}

	public boolean isBlocking() {
		return blocking;
	}

	public PipelinePhase setBlocking(boolean blocking) {
		this.blocking = blocking;
		return this;
	}

	public List<PipelineNode> getJobs() {
		return jobs;
	}

	public PipelinePhase setJobs(List<PipelineNode> jobs) {
		this.jobs = jobs;
		return this;
	}
}