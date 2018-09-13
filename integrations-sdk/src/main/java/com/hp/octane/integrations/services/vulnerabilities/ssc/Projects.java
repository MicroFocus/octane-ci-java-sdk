package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by hijaziy on 7/23/2018.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Projects extends SscBaseEntityArray<Projects.Project> {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Project {
        @JsonProperty("id")
        public Integer id;
        @JsonProperty("name")
        public String name;
    }
}
