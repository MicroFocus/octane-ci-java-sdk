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
package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.vulnerabilities.PackIssuesToOctaneUtils;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.IssueDetails;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Issues;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.hp.octane.integrations.services.vulnerabilities.ssc.SSCToOctaneIssueUtil.createOctaneIssues;


public class PackSSCIssuesToSendToOctane {

    private List<Issues.Issue> sscIssues;
    private List<String> octaneIssues;
    private String remoteTag;
    private boolean considerMissing;
    private SSCHandler sscHandler;


    public List<OctaneIssue> packToOctaneIssues() {

        PackIssuesToOctaneUtils.SortedIssues<Issues.Issue> issueSortedIssues =
                PackIssuesToOctaneUtils.packToOctaneIssues(sscIssues, octaneIssues, considerMissing);

        Map<Integer, IssueDetails> issuesWithExtendedData = sscHandler.getIssuesExtendedData(issueSortedIssues.issuesRequiredExtendedData);
        List<OctaneIssue> openOctaneIssues = createOctaneIssues(issueSortedIssues.issuesToUpdate, remoteTag, issuesWithExtendedData);
        List<OctaneIssue> total = new ArrayList<>();
        total.addAll(openOctaneIssues);
        total.addAll(issueSortedIssues.issuesToClose);

        if (total.isEmpty()) {
            throw new PermanentException("This scan has no issues.");
        }
        return total;
    }

    public void setSscIssues(List<Issues.Issue> sscIssues) {
        this.sscIssues = sscIssues;
    }

    public void setOctaneIssues(List<String> octaneIssues) {
        this.octaneIssues = octaneIssues;
    }

    public void setRemoteTag(String remoteTag) {
        this.remoteTag = remoteTag;
    }

    public void setConsiderMissing(boolean considerMissing) {
        this.considerMissing = considerMissing;
    }

    public void setSscHandler(SSCHandler sscHandler) {
        this.sscHandler = sscHandler;
    }
}
