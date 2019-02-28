package com.hp.octane.integrations.dto.scm;

import com.hp.octane.integrations.dto.scm.impl.RevisionsMap;

public interface SCMFileBlame {

    String getPath();
    RevisionsMap getRevisionsMap();

}
