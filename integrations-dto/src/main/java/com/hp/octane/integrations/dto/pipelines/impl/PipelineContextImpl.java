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
 */

package com.hp.octane.integrations.dto.pipelines.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hp.octane.integrations.dto.general.ListItem;
import com.hp.octane.integrations.dto.general.Taxonomy;
import com.hp.octane.integrations.dto.pipelines.PipelineContext;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
final public class PipelineContextImpl implements PipelineContext {

	private String contextEntityType = "pipeline";//default value
	private long contextEntityId;
	private String contextEntityName;
	private long workspaceId;
	private Long releaseId;
	private Long milestoneId;
	private Boolean ignoreTests;
	private List<Taxonomy> taxonomies;
	private Map<String, List<ListItem>> listFields;
	private Boolean pipelineRoot;

	private Object server;//for creation by octane only
	private Object structure;//for creation by octane only

	@Override
	public long getContextEntityId() {
		return contextEntityId;
	}

	@Override
	public PipelineContext setContextEntityId(long contextEntityId) {
		this.contextEntityId = contextEntityId;
		return this;
	}

	@Override
	public String getContextEntityName() {
		return contextEntityName;
	}

	@Override
	public PipelineContext setContextName(String name) {
		this.contextEntityName = name;
		return this;
	}

	@Override
	public long getWorkspaceId() {
		return workspaceId;
	}

	@Override
	public PipelineContext setWorkspace(long workspaceId) {
		this.workspaceId = workspaceId;
		return this;
	}

	@Override
	public Long getReleaseId() {
		return releaseId;
	}

	@Override
	public PipelineContext setReleaseId(Long releaseId) {
		this.releaseId = releaseId;
		return this;
	}

	@Override
	public Long getMilestoneId() {
		return milestoneId;
	}

	@Override
	public PipelineContext setMilestoneId(Long milestoneId) {
		this.milestoneId = milestoneId;
		return this;
	}

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@Override
	public List<Taxonomy> getTaxonomies() {
		return taxonomies;
	}

	@Override
	public PipelineContext setTaxonomies(List<Taxonomy> taxonomies) {
		this.taxonomies = taxonomies;
		return this;
	}

	@Override
	public Boolean isPipelineRoot() {
		return pipelineRoot;
	}

	@Override
	public PipelineContext setPipelineRoot(Boolean isRoot) {
		this.pipelineRoot = isRoot;
		return this;
	}

	@Override
	public Boolean getIgnoreTests() {
		return ignoreTests;
	}

	@Override
	public PipelineContext setIgnoreTests(Boolean ignoreTests) {
		this.ignoreTests = ignoreTests;
		return this;
	}

	@Override
	public Map<String, List<ListItem>> getListFields() {
		return listFields;
	}

	@Override
	public PipelineContext setListFields(Map<String, List<ListItem>> listFields) {
		this.listFields = listFields;
		return this;
	}

	@Override
	public String getContextEntityType() {
		return contextEntityType;
	}

	@Override
	public PipelineContext setContextEntityType(String contextEntityType) {
		this.contextEntityType = contextEntityType;
		return this;
	}

	public Object getServer() {
		return server;
	}

	public PipelineContext setServer(Object server) {
		this.server = server;
		return this;
	}

	public Object getStructure() {
		return structure;
	}

	public PipelineContext setStructure(Object structure) {
		this.structure = structure;
		return this;
	}
}
