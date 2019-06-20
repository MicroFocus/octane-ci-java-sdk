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

public class OctaneIssueConsts {

    public static final String SEVERITY_LG_NAME_LOW = "list_node.severity.low";
    public static final String SEVERITY_LG_NAME_MEDIUM = "list_node.severity.medium";
    public static final String SEVERITY_LG_NAME_HIGH = "list_node.severity.high";
    public static final String SEVERITY_LG_NAME_CRITICAL = "list_node.severity.urgent";

    public static final String ISSUE_STATE_NEW = "list_node.issue_state_node.new";
    public static final String ISSUE_STATE_EXISTING = "list_node.issue_state_node.existing";
    public static final String ISSUE_STATE_REOPEN = "list_node.issue_state_node.reopen";
    public static final String ISSUE_STATE_CLOSED = "list_node.issue_state_node.closed";

    public static final String NOT_AN_ISSUE = "list_node.issue_analysis_node.not_an_issue";
    public static final String MAYBE_AN_ISSUE = "list_node.issue_analysis_node.maybe_an_issue";
    public static final String IS_AN_ISSUE = "list_node.issue_analysis_node.is_an_issue";
    public static final String REVIEWED = "list_node.issue_analysis_node.reviewed";
    public static final String BUG_SUBMITTED = "list_node.issue_analysis_node.bug_submitted";

    public static boolean isLegalOctaneState(String scanStatus) {
        return ISSUE_STATE_NEW.equals(scanStatus) ||
                ISSUE_STATE_EXISTING.equals(scanStatus) ||
                ISSUE_STATE_REOPEN.equals(scanStatus) ||
                ISSUE_STATE_CLOSED.equals(scanStatus);
    }
}
