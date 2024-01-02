/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
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
package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.exceptions.PermanentException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class PackIssuesToOctaneUtils {

    public static class  SortedIssues<T>{
        public List<T> issuesToUpdate;
        public List<T> issuesRequiredExtendedData;
        public List<OctaneIssue> issuesToClose;

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
