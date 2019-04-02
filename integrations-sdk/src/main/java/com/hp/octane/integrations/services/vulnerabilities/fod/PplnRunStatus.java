package com.hp.octane.integrations.services.vulnerabilities.fod;

public class PplnRunStatus {
    boolean continuePolling;
    boolean tryGetIssues;

    public PplnRunStatus(boolean continuePolling, boolean tryGetIssues) {
        this.continuePolling = continuePolling;
        this.tryGetIssues = tryGetIssues;
    }
}