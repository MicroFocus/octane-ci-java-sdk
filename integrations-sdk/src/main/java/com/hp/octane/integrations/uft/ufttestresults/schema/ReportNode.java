package com.hp.octane.integrations.uft.ufttestresults.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.List;

public class ReportNode {

    private String type;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JsonProperty("ReportNode")
    private List<ReportNode> nodes;

    @JsonProperty("Data")
    private ReportNodeData data;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<ReportNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<ReportNode> nodes) {
        this.nodes = nodes;
    }

    public ReportNodeData getData() {
        return data;
    }

    public void setData(ReportNodeData data) {
        this.data = data;
    }
}
