package com.hp.octane.integrations.uft.ufttestresults.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class ReportResults {

    @JacksonXmlProperty(isAttribute = true, localName = "version")
    String version;

    @JsonProperty("ReportNode")
    ReportNode reportNode;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ReportNode getReportNode() {
        return reportNode;
    }

    public void setReportNode(ReportNode reportNode) {
        this.reportNode = reportNode;
    }
}
