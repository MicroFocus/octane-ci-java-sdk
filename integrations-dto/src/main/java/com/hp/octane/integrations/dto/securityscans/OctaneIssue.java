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

    void setToolName(String category);

    String getCategory();

    void setCategory(String category);

    String getPackage();

    void setPackage(String package1);

    String getRemoteTag();

    void setRemoteTag(String remote_tag);

    String getOwnerEmail();
    void setOwnerEmail(String ownerEmail);

}
