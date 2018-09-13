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
package com.hp.octane.integrations.dto.securityscans;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.entities.Entity;

import java.util.Map;

public interface OctaneIssue extends DTOBase {
    void setExtendedData(Map extendedData);

    void setPrimaryLocationFull(String primaryLocationFull);

    void setLine(Integer line);

    void setAnalysis(Entity analysis);

    void setState(Entity state);

    void setSeverity(Entity severity);

    Map getExtendedData();

    String getPrimaryLocationFull();

    Integer getLine();

    Entity getAnalysis();

    Entity getState();

    Entity getSeverity();

    String getRemoteId();

    void setRemoteId(String remote_id);

    String getIntroducedDate();

    void setIntroducedDate(String introducedDate);

    String getExternalLink();

    void setExternalLink(String external_link);

    String getToolName();

    void setToolName(String toolName);
}
