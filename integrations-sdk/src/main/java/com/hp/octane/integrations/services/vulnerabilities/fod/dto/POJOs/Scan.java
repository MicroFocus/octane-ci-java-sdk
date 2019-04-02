package com.hp.octane.integrations.services.vulnerabilities.fod.dto.POJOs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.FODEntityCollection;

import java.io.Serializable;


/**
 * Created by hijaziy on 7/30/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Scan  implements Serializable{

    public final static String IN_PROGRESS = "In_Progress";
    public final static String COMPLETED = "Completed";
    public final static String NOT_STARTED = "Not_Started";

    @JsonProperty("scanId")
    public Long scanId;

    @JsonProperty("releaseId")
    public Long releaseId;


    @JsonProperty("analysisStatusType")
    public String status;

    @JsonProperty("startedDateTime")
    public String startedDateTime;

    @JsonProperty("completedDateTime")
    public String completedDateTime;

    @JsonProperty("notes")
    public String notes;

    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class Scans extends FODEntityCollection<Scan> {

    }

}
