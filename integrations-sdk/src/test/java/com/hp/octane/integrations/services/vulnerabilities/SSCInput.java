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
package com.hp.octane.integrations.services.vulnerabilities;


import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Artifacts;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Issues;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class SSCInput {

    public String projectName;
    public Integer projectId;
    public String  projectVersionName;
    public Integer projectVersionId;
    public Artifacts artifacts;
    public Issues issuesToReturn;


    public SSCInput withProject(String projectName, int projectId){
        this.projectName = projectName;
        this.projectId = projectId;
        return this;
    }
    public SSCInput withProjectVersion(String projectVersionName, int projectVersionId){
        this.projectVersionName = projectVersionName;
        this.projectVersionId = projectVersionId;
        return this;
    }

    public SSCInput setArtifactsPage(Artifacts artifacts){
        this.artifacts = artifacts;
        return this;
    }

    public Issues getIssuesToReturn() {
        return issuesToReturn;
    }

    public void setIssuesToReturn(Issues issuesToReturn) {
        this.issuesToReturn = issuesToReturn;
    }
    public static SSCInput buildInputWithDefaults(List<Issues.Issue> issuesToReturn){
        SSCInput sscInput = new SSCInput();
        sscInput.withProject("project-a",2);
        sscInput.withProjectVersion("version-a",22);

        Artifacts artifacts = buildArtifactsInput(System.currentTimeMillis());
        sscInput.setArtifactsPage(artifacts);

        Issues issues = new Issues();
        issues.setData(new ArrayList<>());
        issues.getData().addAll(issuesToReturn);
        issues.setCount(issues.getData().size());
        sscInput.setIssuesToReturn(issues);
        return sscInput;
    }
    private static Artifacts buildArtifactsInput(long startTimeOfBuild) {
        return createArtifacts(startTimeOfBuild, "PROCESS_COMPLETE");
    }

    public static Artifacts createArtifacts(long startTimeOfBuild, String artifactStatus) {
        Artifacts.Artifact artifact = new Artifacts.Artifact();
        artifact.id =1;
        artifact.status = artifactStatus;
        DateFormat sourceDateFormat = new SimpleDateFormat(DateUtils.sscFormat);
        Date date = new Date(startTimeOfBuild + 7000);
        artifact.uploadDate = sourceDateFormat.format(date);
        Artifacts artifacts = new Artifacts();
        artifacts.setData(Arrays.asList(artifact));
        artifacts.setCount(1);
        return artifacts;
    }
}
