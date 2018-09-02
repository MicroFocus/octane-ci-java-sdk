package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Artifacts extends SscBaseEntityArray<Artifacts.Artifact>  {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Artifact{
        @JsonProperty("status")
        public String status;
        @JsonProperty("id")
        public Integer id;
        @JsonProperty("uploadDate")
        public String uploadDate;
        //"IGNORED"
        @JsonProperty("scaStatus")
        public String scaStatus;

    }
}
