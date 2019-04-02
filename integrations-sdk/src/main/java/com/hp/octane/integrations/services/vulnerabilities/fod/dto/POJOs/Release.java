package com.hp.octane.integrations.services.vulnerabilities.fod.dto.POJOs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.FODEntityCollection;

/**
 * Created by hijaziy on 8/10/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Release {


    @JsonProperty("releaseId")
    public Long releaseId;

    @JsonProperty("releaseName")
    public String releaseName;


    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class Releases extends FODEntityCollection<Release> {

    }

}
