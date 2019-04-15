package com.hp.octane.integrations.dto.scm;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.scm.impl.RevisionsMap;

public interface SCMFileBlame extends DTOBase {

    String getPath();
    RevisionsMap getRevisionsMap();

}
