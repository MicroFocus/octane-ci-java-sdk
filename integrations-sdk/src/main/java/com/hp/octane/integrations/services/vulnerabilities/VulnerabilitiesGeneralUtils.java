package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.dto.securityscans.impl.OctaneIssueImpl;

import static com.hp.octane.integrations.services.vulnerabilities.ssc.SSCToOctaneIssueUtil.createListNodeEntity;

public class VulnerabilitiesGeneralUtils {
    public static OctaneIssue createClosedOctaneIssue(String remoteId) {
        Entity closedListNodeEntity = createListNodeEntity(OctaneIssueConsts.ISSUE_STATE_CLOSED);
        OctaneIssueImpl octaneIssue = new OctaneIssueImpl();
        octaneIssue.setRemoteId(remoteId);
        octaneIssue.setState(closedListNodeEntity);
        return octaneIssue;
    }
}
