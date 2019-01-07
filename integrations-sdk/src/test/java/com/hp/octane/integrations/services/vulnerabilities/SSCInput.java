package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.services.vulnerabilities.ssc.Artifacts;
import com.hp.octane.integrations.services.vulnerabilities.ssc.Issues;
import com.hp.octane.integrations.services.vulnerabilities.ssc.SSCDateUtils;

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
        DateFormat sourceDateFormat = new SimpleDateFormat(SSCDateUtils.sscFormat);
        Date date = new Date(startTimeOfBuild + 7000);
        artifact.uploadDate = sourceDateFormat.format(date);
        Artifacts artifacts = new Artifacts();
        artifacts.setData(Arrays.asList(artifact));
        artifacts.setCount(1);
        return artifacts;
    }
}
