package com.hp.octane.integrations.dto.scm.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RevisionsMap extends HashMap<String, List<LineRange>> {

    @JsonIgnore
    public List<LineRange> getRangeList(String revision){
        return this.get(revision);
    }

    @JsonIgnore
    public void setRangeList(String revision, List<LineRange> rangeList) {
        this.put(revision,rangeList);
    }

    public void addRangeToRevision(String revision, LineRange lineRange ){

        List<LineRange> rangeList =  this.get(revision);
        if (rangeList == null)
        {
            rangeList = new ArrayList<>();
            put(revision,rangeList);
        }
        rangeList.add(lineRange);
    }
}
