package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.exceptions.PermanentException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class PackIssuesToOctaneUtils {

    public static class  SortedIssues<T>{
        public SortedIssues(){
            this.issuesToUpdate = new ArrayList<>();
            this.issuesToClose = new ArrayList<>();
            this.issuesRequiredExtendedData = new ArrayList<>();
        }
        public SortedIssues(List<T> issuesToUpdate, List<OctaneIssue> issuesToClose, List<T> issuesRequiredExtendedData) {
            this.issuesToUpdate = issuesToUpdate;
            this.issuesToClose = issuesToClose;
            this.issuesRequiredExtendedData = issuesRequiredExtendedData;
        }
        public List<T> issuesToUpdate;
        public List<T> issuesRequiredExtendedData;
        public List<OctaneIssue> issuesToClose;
    }


    public static <T extends RawVulnerability> SortedIssues<T> packToOctaneIssues(List<T> rawIssues,
                                                         List<String> octaneIssues,
                                                         boolean calcMissing) {
        if (rawIssues.size() == 0 && octaneIssues.size() == 0) {
            throw new PermanentException("This job run has no issues.");
        }
        List<T> issuesRequiredExtendedData = rawIssues.stream().filter(
                t -> {
                    boolean isMissing = false;
                    if (calcMissing) {
                        isMissing = !octaneIssues.contains(t.getRemoteId());
                    }
                    return t.isNew() || isMissing;
                }).collect(
                Collectors.toList());

        List<String> remoteIdsOfRawIssues =
                rawIssues.stream().map(t -> t.getRemoteId()).collect(Collectors.toList());

        List<String> remoteIdsToCloseInOctane = octaneIssues.stream()
                .filter(t -> !remoteIdsOfRawIssues.contains(t))
                .collect(Collectors.toList());

        //Make Octane issue from remote id's.
        List<OctaneIssue> closedOctaneIssues = remoteIdsToCloseInOctane.stream()
                .map(VulnerabilitiesGeneralUtils::createClosedOctaneIssue).collect(Collectors.toList());

        //Issues that are not closed , packed to update/create.
        List<T> issuesToUpdate = rawIssues.stream()
                .filter(t -> !remoteIdsToCloseInOctane.contains(t.getRemoteId()))
                .collect(Collectors.toList());
        if(issuesToUpdate.size() == 0 && closedOctaneIssues.size() == 0) {
            throw new PermanentException("This job run has no issues.");
        }
        return new SortedIssues<>(issuesToUpdate, closedOctaneIssues, issuesRequiredExtendedData);
    }

}
