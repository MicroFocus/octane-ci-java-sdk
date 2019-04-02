package com.hp.octane.integrations.services.vulnerabilities.fod.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hijaziy on 8/2/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class FODEntityCollection<T> implements Serializable{

    @JsonProperty("items")
    public List<T> items = new ArrayList<T>();

    @JsonProperty("totalCount")
    int totalCount;

}
