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

package com.hp.octane.integrations.dto.pipelines;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.general.ListItem;
import com.hp.octane.integrations.dto.general.Taxonomy;

import java.util.List;
import java.util.Map;

public interface PipelineContext extends DTOBase {

    long getContextEntityId();

    PipelineContext setContextEntityId(long id);

    String getContextEntityName();

    PipelineContext setContextName(String name);

    long getWorkspaceId();

    PipelineContext setWorkspace(long workspaceId);

    Long getReleaseId();

    PipelineContext setReleaseId(Long releaseId);

    Long getMilestoneId();

    PipelineContext setMilestoneId(Long milestoneId);

    List<Taxonomy> getTaxonomies();

    PipelineContext setTaxonomies(List<Taxonomy> taxonomies);

    Map<String, List<ListItem>> getListFields();

    PipelineContext setListFields(Map<String, List<ListItem>> listFields);

    Boolean isPipelineRoot();

    PipelineContext setPipelineRoot(Boolean isRoot);

    Boolean getIgnoreTests();

    PipelineContext setIgnoreTests(Boolean ignoreTests);

    String getContextEntityType();

    PipelineContext setContextEntityType(String contextEntityType);

    Object getServer();

    PipelineContext setServer(Object server);

    Object getStructure();

    PipelineContext setStructure(Object structure);
}
