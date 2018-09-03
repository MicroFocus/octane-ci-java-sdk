package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by hijaziy on 7/23/2018.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectVersions extends SscBaseEntityArray<ProjectVersions.ProjectVersion> {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProjectVersion {
        @JsonProperty("id")
        public Integer id;
        @JsonProperty("latestScanId")
        public Integer latestScanId;
    }
}
