package com.hp.octane.integrations.dto.scm.impl;

import com.hp.octane.integrations.dto.scm.SCMFileBlame;


public class SCMFileBlameImpl implements SCMFileBlame {

    private String path;
    private RevisionsMap revisionsMap;

    public SCMFileBlameImpl(){

    }

     public SCMFileBlameImpl(String path, RevisionsMap revisionsMap) {
         this.path = path;
         this.revisionsMap = revisionsMap;
     }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public RevisionsMap getRevisionsMap() {
        return this.revisionsMap;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setRevisionsMap(RevisionsMap revisionsMap) {
        this.revisionsMap = revisionsMap;
    }
}
