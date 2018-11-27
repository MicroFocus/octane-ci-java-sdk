package com.hp.octane.integrations.dto.scm;

import com.hp.octane.integrations.dto.scm.impl.LineRange;
import com.hp.octane.integrations.dto.scm.impl.RevisionsMap;

import java.util.List;
import java.util.Map;

public interface SCMFileBlame {

    String getPath();
    RevisionsMap getRevisionsMap();

}
