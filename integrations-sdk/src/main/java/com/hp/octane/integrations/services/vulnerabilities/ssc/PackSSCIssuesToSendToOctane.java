/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.vulnerabilities.PackIssuesToOctaneUtils;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.IssueDetails;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Issues;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.hp.octane.integrations.services.vulnerabilities.ssc.SSCToOctaneIssueUtil.createOctaneIssues;


public class PackSSCIssuesToSendToOctane {

    private static final Logger logger = LogManager.getLogger(PackSSCIssuesToSendToOctane.class);

    private List<Issues.Issue> sscIssues;
    private List<String> octaneIssues;
    private String remoteTag;
    private boolean considerMissing;
    private SSCHandler sscHandler;


    public List<OctaneIssue> packToOctaneIssues() {

        logger.debug("started packing");
        PackIssuesToOctaneUtils.SortedIssues<Issues.Issue> issueSortedIssues =
                PackIssuesToOctaneUtils.packToOctaneIssues(sscIssues, octaneIssues, considerMissing);

        Map<Integer, IssueDetails> issuesWithExtendedData = sscHandler.getIssuesExtendedData(issueSortedIssues.issuesRequiredExtendedData);
        logger.debug("before creating octane issues");
        List<OctaneIssue> openOctaneIssues = createOctaneIssues(issueSortedIssues.issuesToUpdate, remoteTag, issuesWithExtendedData);
        logger.debug("after creating octane issues");

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
