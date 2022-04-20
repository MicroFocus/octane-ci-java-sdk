package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.CIBranchesList;
import com.hp.octane.integrations.dto.scm.Branch;

import java.util.List;

/**
 * CIBranchesList DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class CIBranchesListImpl implements CIBranchesList {

    private List<Branch> branches;

    @Override
    public List<Branch> getBranches() {
        return branches;
    }

    @Override
    public CIBranchesList setBranches(List<Branch> branches) {
        this.branches = branches;
        return this;
    }
}

