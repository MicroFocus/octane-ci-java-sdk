package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.services.vulnerabilities.ssc.Issues;

import java.util.ArrayList;
import java.util.List;

public class ExpectedPushToOctane {
    public List<Issues.Issue> updateIssues = new ArrayList<>();
    public List<Issues.Issue> newIssues = new ArrayList<>();
    public List<String> closedIssuesStillExistingInOctane = new ArrayList<>();
    public List<Issues.Issue> beforeBaselineIssues = new ArrayList<>();
    public List<Issues.Issue> missingIssues = new ArrayList<>();
    public boolean missingHasExtendedData = true;
    public boolean expectNoPsuh = false;
}
