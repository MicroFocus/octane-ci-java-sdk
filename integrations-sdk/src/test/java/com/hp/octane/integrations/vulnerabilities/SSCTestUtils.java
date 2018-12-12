package com.hp.octane.integrations.vulnerabilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.services.vulnerabilities.ssc.Artifacts;
import com.hp.octane.integrations.services.vulnerabilities.ssc.ProjectVersions;
import com.hp.octane.integrations.services.vulnerabilities.ssc.Projects;
import com.hp.octane.integrations.services.vulnerabilities.ssc.SSCDateUtils;

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
        DateFormat sourceDateFormat = new SimpleDateFormat(SSCDateUtils.sscFormat);
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
