package com.hp.octane.integrations.services.pullrequestsandbranches.factory;

public class RepoTemplates {

    private String sourceViewTemplate;
    private String diffTemplate;
    private String branchFileTemplate;

    public String getSourceViewTemplate() {
        return sourceViewTemplate;
    }

    public void setSourceViewTemplate(String sourceViewTemplate) {
        this.sourceViewTemplate = sourceViewTemplate;
    }

    public String getDiffTemplate() {
        return diffTemplate;
    }

    public void setDiffTemplate(String diffTemplate) {
        this.diffTemplate = diffTemplate;
    }

    public String getBranchFileTemplate() {
        return branchFileTemplate;
    }

    public void setBranchFileTemplate(String branchFileTemplate) {
        this.branchFileTemplate = branchFileTemplate;
    }
}
