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
    public static final String REVIEWED = "list_node.issue_analysis_node.reviewed";
    public static final String BUG_SUBMITTED = "list_node.issue_analysis_node.bug_submitted";

    public static boolean isLegalOctaneState(String scanStatus) {
        return ISSUE_STATE_NEW.equals(scanStatus) ||
                ISSUE_STATE_EXISTING.equals(scanStatus) ||
                ISSUE_STATE_REOPEN.equals(scanStatus) ||
                ISSUE_STATE_CLOSED.equals(scanStatus);
    }
}
