package com.hp.octane.integrations.dto.scm;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.scm.impl.RevisionsMap;

import java.io.Serializable;

public interface SCMFileBlame extends DTOBase, Serializable {

    String getPath();
    RevisionsMap getRevisionsMap();

}
