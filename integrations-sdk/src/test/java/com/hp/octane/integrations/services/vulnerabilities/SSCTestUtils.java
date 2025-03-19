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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Artifacts;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.ProjectVersions;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Projects;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class SSCTestUtils {

    public static String getJson(Object ObjToWrite) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        StringWriter stringWriter = new StringWriter();
        objectMapper.writeValue(stringWriter, ObjToWrite);
        stringWriter.flush();
        String jsonVal = stringWriter.toString();
        stringWriter.close();
        return jsonVal;
    }

    public static String getArtificatResponse(String status) throws IOException {
        Artifacts artifacts = new Artifacts();
        Artifacts.Artifact artifact = new Artifacts.Artifact();
        DateFormat sourceDateFormat = new SimpleDateFormat(DateUtils.sscFormat);
        artifact.uploadDate = sourceDateFormat.format(new Date());
        artifact.status = status;
        artifacts.setData(Arrays.asList(artifact));
        artifacts.setCount(1);
        return SSCTestUtils.getJson(artifacts);
    }

    public static String getProjectVersionResponse() throws IOException {
        ProjectVersions projectVersions = new ProjectVersions();
        ProjectVersions.ProjectVersion projectVersion = new ProjectVersions.ProjectVersion();
        projectVersion.id = 100;
        projectVersions.setData(Arrays.asList(projectVersion));
        projectVersions.setCount(1);
        return SSCTestUtils.getJson(projectVersions);
    }

    public static String getDummyProjectResponse() throws IOException {
        Projects projects = new Projects();
        Projects.Project project = new Projects.Project();
        project.name = "ABC";
        project.id = 10;
        projects.setData(Arrays.asList(project));
        projects.setCount(1);
        return SSCTestUtils.getJson(projects);
    }

}
