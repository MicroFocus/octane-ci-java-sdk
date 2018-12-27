/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.dto.securityscans.impl.OctaneIssueImpl;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.vulnerabilities.ssc.IssueDetails;
import com.hp.octane.integrations.services.vulnerabilities.ssc.Issues;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hp.octane.integrations.services.vulnerabilities.SSCToOctaneIssueUtil.createListNodeEntity;
import static com.hp.octane.integrations.services.vulnerabilities.SSCToOctaneIssueUtil.createOctaneIssues;


public class PackIssuesToSendToOctane {

    public List<OctaneIssue> packAllIssues(List<Issues.Issue> sscIssues, List<String> existingIssuesInOctane, String remoteTag, Map<Integer,IssueDetails> issueDetailsById) {
        List<OctaneIssue> octaneIssues = packToOctaneIssues(sscIssues,existingIssuesInOctane, remoteTag, issueDetailsById);
        if (octaneIssues.isEmpty()) {
            throw new PermanentException("This scan has no issues.");
        }
        return octaneIssues;
    }

    private List<OctaneIssue> packToOctaneIssues(List<Issues.Issue> sscIssues, List<String> octaneIssues, String remoteTag, Map<Integer, IssueDetails> issueDetailsById) {
        if (sscIssues.size() == 0 && octaneIssues.size() == 0) {
            return new ArrayList<>();
        }
        List<String> remoteIdsSSC =
                sscIssues.stream().map(t -> t.issueInstanceId).collect(Collectors.toList());

        List<String> remoteIdsToCloseInOctane = octaneIssues.stream()
                .filter(t -> !remoteIdsSSC.contains(t))
                .collect(Collectors.toList());

        //Make Octane issue from remote id's.
        List<OctaneIssue> closedOctaneIssues = remoteIdsToCloseInOctane.stream()
                .map(t -> createClosedOctaneIssue(t)).collect(Collectors.toList());

        //Issues that are not closed , packed to update/create.
        List<Issues.Issue> issuesToUpdate = sscIssues.stream()
                .filter(t -> !remoteIdsToCloseInOctane.contains(t.issueInstanceId))
                .collect(Collectors.toList());
        //Issues.Issue
        List<OctaneIssue> openOctaneIssues = createOctaneIssues(issuesToUpdate, remoteTag, issueDetailsById);
        List<OctaneIssue> total = new ArrayList<>();
        total.addAll(openOctaneIssues);
        total.addAll(closedOctaneIssues);
        return total;
    }

    private OctaneIssue createClosedOctaneIssue(String remoteId) {
        Entity closedListNodeEntity = createListNodeEntity("list_node.issue_state_node.closed");
        OctaneIssueImpl octaneIssue = new OctaneIssueImpl();
        octaneIssue.setRemoteId(remoteId);
        octaneIssue.setState(closedListNodeEntity);
        return octaneIssue;
    }

}
