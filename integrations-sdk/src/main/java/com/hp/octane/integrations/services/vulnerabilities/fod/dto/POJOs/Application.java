package com.hp.octane.integrations.services.vulnerabilities.fod.dto.POJOs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.FODEntityCollection;


/**
 * Created by hijaziy on 8/10/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Application {

    @JsonProperty("applicationId")
    public Long applicationId;

    @JsonProperty("applicationName")
    public String applicationName;

    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class Applications extends FODEntityCollection<Application> {

    }
}
