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
package com.hp.octane.integrations.dto.SecurityScans;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.entities.Entity;

import java.util.Map;

public interface OctaneIssue extends DTOBase {
    void setExtended_data(Map extendedData);
    void setPrimary_location_full(String primaryLocationFull);
    void setLine(Integer line);
    void setAnalysis(Entity analysis);
    void setState(Entity state);
    void setSeverity(Entity severity);
    Map getExtended_data();
    String getPrimary_location_full();
    Integer getLine();
    Entity getAnalysis();
    Entity getState();
    Entity getSeverity();
    String getRemote_id();
    void setRemote_id(String remote_id);
    String getIntroduced_date();
    void setIntroduced_date(String introducedDate);
    String getExternal_link();
    void setExternal_link(String external_link);
    String getTool_name();
    void setTool_name(String toolName);
}
