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
 */

package com.hp.octane.integrations.services.vulnerabilities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Issues;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IssuesValidate {

    public static class SSCTestFailure extends Throwable{
        public String sscFailureMessage;
        public SSCTestFailure(String sscFailureMsg){
            this.sscFailureMessage = sscFailureMsg;
        }
        public String getSSCFailureMessage(){
            return sscFailureMessage;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OctaneIssuesPushed{
         @JsonProperty("data")
         public List<OctaneIssue> octaneIssues;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RawPushedIssues{
        @JsonProperty("data")
        public List<Map> data;
    }

    public void validateOutput(String output, ExpectedPushToOctane expectedPushToOctane) throws IOException,
            SSCTestFailure {

        OctaneIssuesPushed octaneIssuesPushed = getOctaneIssuesPushed(output);
        validateOctaneRequests(octaneIssuesPushed, expectedPushToOctane);
    }

    private OctaneIssuesPushed getOctaneIssuesPushed(String output) throws IOException {
        OctaneIssuesPushed octaneIssuesPushed = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            RawPushedIssues rawPushedIssues = objectMapper.readValue(output, RawPushedIssues.class);
            StringWriter stringWriter = new StringWriter();
            objectMapper.writeValue(stringWriter,rawPushedIssues.data);
            stringWriter.flush();
            String issuesArrayAsString = stringWriter.toString();

            OctaneIssue[] octaneIssuesArray = DTOFactory.getInstance().dtoCollectionFromJson(issuesArrayAsString, OctaneIssue[].class);
            octaneIssuesPushed = new OctaneIssuesPushed();
            octaneIssuesPushed.octaneIssues = Arrays.asList(octaneIssuesArray);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return octaneIssuesPushed;
    }

    private void validateOctaneRequests(OctaneIssuesPushed octaneIssuesPushed,
                                              ExpectedPushToOctane expectedPushToOctane) throws SSCTestFailure {

        validateTotalIssuesByStateId(octaneIssuesPushed, expectedPushToOctane);
        validateExisting(octaneIssuesPushed, expectedPushToOctane.updateIssues);
        validateNew(octaneIssuesPushed, expectedPushToOctane.newIssues);
        validateClosed(octaneIssuesPushed, expectedPushToOctane.closedIssuesStillExistingInOctane);
        validateBeforeBaseline(octaneIssuesPushed, expectedPushToOctane.beforeBaselineIssues);
        validateMissing(octaneIssuesPushed,
                expectedPushToOctane.missingIssues,
                expectedPushToOctane.missingHasExtendedData);

    }

    private void validateTotalIssuesByStateId(OctaneIssuesPushed octaneIssuesPushed,
                                              ExpectedPushToOctane expectedPushToOctane) throws SSCTestFailure {

        String updatedStateId = "list_node.issue_state_node.existing";
        List<Issues.Issue> updatedIssues = new ArrayList<>();
        updatedIssues.addAll(expectedPushToOctane.missingIssues);
        updatedIssues.addAll(expectedPushToOctane.updateIssues);
        validateIssuesOfState(octaneIssuesPushed, updatedIssues.size(), updatedStateId);

        String newStateId = "list_node.issue_state_node.new";
        validateIssuesOfState(octaneIssuesPushed, expectedPushToOctane.newIssues.size(), newStateId);

        String closedStateId = "list_node.issue_state_node.closed";
        validateIssuesOfState(octaneIssuesPushed,
                expectedPushToOctane.closedIssuesStillExistingInOctane.size(),
                closedStateId);

    }

    private void validateBeforeBaseline(OctaneIssuesPushed octaneIssuesPushed,
                                              List<Issues.Issue> beforeBaselineIssues) throws SSCTestFailure {
        List<String> remoteIdsOfBeforeBaseline = beforeBaselineIssues.stream().map(t -> t.issueInstanceId).collect(Collectors.toList());
        if(octaneIssuesPushed.octaneIssues.stream().anyMatch(t->remoteIdsOfBeforeBaseline.contains(t.getRemoteId()))){
            throw new SSCTestFailure("Issues before baseline were pushed to Octane.");
        }
    }

    private void validateClosed(OctaneIssuesPushed octaneIssuesPushed,
                                      List<String> closedIssuesNotExistingInOctane) throws SSCTestFailure {

        for (String closedIssue : closedIssuesNotExistingInOctane) {
            List<OctaneIssue> octaneIssues = octaneIssuesPushed.octaneIssues.stream()
                    .filter(t -> closedIssue.equals(t.getRemoteId()))
                    .collect(Collectors.toList());
            if(octaneIssues.size()!= 1){
                throw new SSCTestFailure("Closed Issue was not pushed to Octane.");
            }
            validateState(octaneIssues.get(0), "list_node.issue_state_node.closed",
                    "Close Issue was pushed to Octane but not with the right state.");
        }
    }

    private void validateState(OctaneIssue octaneIssue, String s, String errorMsg) throws SSCTestFailure {
        if (!octaneIssue.getState().getId().equals(s)) {
            throw new SSCTestFailure(errorMsg);
        }
    }

    private void validateNew(OctaneIssuesPushed octaneIssuesPushed, List<Issues.Issue> newIssues) throws SSCTestFailure {


        for (Issues.Issue newIssue : newIssues) {
            List<OctaneIssue> newOctaneIssue = octaneIssuesPushed.octaneIssues.stream()
                    .filter(t -> newIssue.issueInstanceId.equals(t.getRemoteId()))
                    .collect(Collectors.toList());

            if(newOctaneIssue.size()!= 1){
                throw new SSCTestFailure("New Issue was not pushed to Octane.");
            }

            validateState(newOctaneIssue.get(0),
                    "list_node.issue_state_node.new",
                    "New Issue was pushed to Octane but not with the right state.");

            validateExtendedDataContainsAllData(newOctaneIssue.get(0));
        }
    }

    private  void validateExtendedDataContainsAllData(OctaneIssue octaneIssue) throws SSCTestFailure {

        if(octaneIssue.getExtendedData().get("summary") == null ||
                octaneIssue.getExtendedData().get("explanation") == null ||
                octaneIssue.getExtendedData().get("recommendations") == null ||
                octaneIssue.getExtendedData().get("tips") == null){
            String state = octaneIssue.getState().getId().contains("new") ? "new" : "missing";
            throw new SSCTestFailure("Issue Details was missing in " + state +" issue");
        }
    }

    private  void validateIssueDoesNotContainsIssueDetails(OctaneIssue octaneIssue) throws SSCTestFailure {

        if(octaneIssue.getExtendedData().get("summary") != null ||
                octaneIssue.getExtendedData().get("explanation") != null ||
                octaneIssue.getExtendedData().get("recommendations") != null ||
                octaneIssue.getExtendedData().get("tips") != null){
            throw new SSCTestFailure("Issue Details was found in an existing - not missing - issue");
        }
    }

    private void validateExisting(OctaneIssuesPushed octaneIssuesPushed, List<Issues.Issue> existingIssues)
            throws SSCTestFailure {


        for (Issues.Issue existingIssue : existingIssues) {
            List<OctaneIssue> existingOctaneIssue = octaneIssuesPushed.octaneIssues.stream()
                    .filter(t -> existingIssue.issueInstanceId.equals(t.getRemoteId()))
                    .collect(Collectors.toList());

            if(existingOctaneIssue.size()!= 1){
                throw new SSCTestFailure("Existing Issue was not pushed to Octane.");
            }

            validateState(existingOctaneIssue.get(0),
                    "list_node.issue_state_node.existing",
                    "Existing Issue was pushed to Octane but not with the right state.");
            validateIssueDoesNotContainsIssueDetails(existingOctaneIssue.get(0));

        }
    }

    private void validateIssuesOfState(OctaneIssuesPushed octaneIssuesPushed,
                                       int numberInPushedIssues, String stateId) throws SSCTestFailure {
        List<OctaneIssue> pushedIssues = octaneIssuesPushed.octaneIssues.stream()
                .filter(t -> t.getState().getId().equals(stateId))
                .collect(Collectors.toList());

        if(pushedIssues.size() != numberInPushedIssues){
            throw new SSCTestFailure("Unexpected number of " + stateId+ " issues were pushed");
        }
    }

    private void validateMissing(OctaneIssuesPushed octaneIssuesPushed, List<Issues.Issue> missingIssues,
                                 boolean checkExtenedData) throws SSCTestFailure {

        for (Issues.Issue missingIssue : missingIssues) {
            List<OctaneIssue> missingOctaneIssue = octaneIssuesPushed.octaneIssues.stream()
                    .filter(t -> missingIssue.issueInstanceId.equals(t.getRemoteId()))
                    .collect(Collectors.toList());

            if(missingOctaneIssue.size()!= 1){
                throw new SSCTestFailure("Missing Updated Issue was not pushed to Octane.");
            }

            validateState(missingOctaneIssue.get(0),
                    "list_node.issue_state_node.existing",
                    "Missing Updated Issue was pushed to Octane but not with the right state.");

            if(checkExtenedData) {
                validateExtendedDataContainsAllData(missingOctaneIssue.get(0));
            }
        }


    }

}
