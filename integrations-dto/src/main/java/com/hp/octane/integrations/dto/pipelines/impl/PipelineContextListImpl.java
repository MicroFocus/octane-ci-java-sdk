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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.pipelines.PipelineContext;
import com.hp.octane.integrations.dto.pipelines.PipelineContextList;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PipelineContextListImpl implements PipelineContextList {

    private List<PipelineContext> data;

    @Override
    public List<PipelineContext> getData() {
        return data;
    }

    @Override
    public PipelineContextList setData(List<PipelineContext> data) {
        this.data = data;
        return this;
    }

    public Map<Long, List<PipelineContext>> buildWorkspace2PipelinesMap() {
        Map<Long, List<PipelineContext>> ret = new HashMap<Long, List<PipelineContext>>();
        for (PipelineContext pipeline : data) {
            if (ret.containsKey(pipeline.getWorkspaceId())) {
                ret.get(pipeline.getWorkspaceId()).add(pipeline);
            } else {
                ret.put(pipeline.getWorkspaceId(), new LinkedList<PipelineContext>(Arrays.asList(pipeline)));
            }
        }
        return ret;
    }
}
