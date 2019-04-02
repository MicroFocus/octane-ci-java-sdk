package com.hp.octane.integrations.services.vulnerabilities.fod;

public class PplnRunStatus {

    public PplnRunStatus(boolean continuePolling, boolean tryGetIssues) {
        this.continuePolling = continuePolling;
        this.tryGetIssues = tryGetIssues;
    }

    boolean continuePolling;
    boolean tryGetIssues;
}