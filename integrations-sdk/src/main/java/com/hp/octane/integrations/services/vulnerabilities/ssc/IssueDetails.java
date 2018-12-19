package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueDetails extends SscBaseEntitySingle<IssueDetails.IssueDetailsData>{
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IssueDetailsData{
        @JsonProperty("recommendation")
        public String recommendation;
        @JsonProperty("detail")
        public String detail;
        @JsonProperty("brief")
        public String brief;
        @JsonProperty("tips")
        public String tips;
    }
}
