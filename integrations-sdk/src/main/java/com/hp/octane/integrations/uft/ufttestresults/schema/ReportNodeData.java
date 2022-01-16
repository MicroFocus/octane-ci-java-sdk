package com.hp.octane.integrations.uft.ufttestresults.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ReportNodeData {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("ErrorText")
    private String errorText;

    @JsonProperty("Duration")
    private long duration;

    @JsonProperty("Result")
    private String result;

    @JsonProperty("InputParameters")
    private List<Parameter> inputParameters;

    @JsonProperty("OutputParameters")
    private List<Parameter> outputParameters;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public List<Parameter> getInputParameters() {
        return inputParameters;
    }

    public void setInputParameters(List<Parameter> inputParameters) {
        this.inputParameters = inputParameters;
    }

    public List<Parameter> getOutputParameters() {
        return outputParameters;
    }

    public void setOutputParameters(List<Parameter> outputParameters) {
        this.outputParameters = outputParameters;
    }
}
