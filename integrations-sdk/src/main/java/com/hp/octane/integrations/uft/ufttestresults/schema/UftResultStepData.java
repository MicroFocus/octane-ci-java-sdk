package com.hp.octane.integrations.uft.ufttestresults.schema;

import java.io.Serializable;
import java.util.List;

public class UftResultStepData implements Serializable {
    private List<String> parents;
    private String type;
    private String result;
    private String message;
    private long duration;
    private List<UftResultStepParameter> inputParameters;
    private List<UftResultStepParameter> outputParameters;

    public UftResultStepData(List<String> parents, String type, String result, String message, long duration) {
        this.parents = parents;
        this.type = type;
        this.result = result;
        this.message = message;
        this.duration = duration;
    }

    public UftResultStepData(List<String> parents, String type, String result, String message, long duration, List<UftResultStepParameter> inputParameters, List<UftResultStepParameter> outputParameters) {
        this.parents = parents;
        this.type = type;
        this.result = result;
        this.message = message;
        this.duration = duration;
        this.inputParameters = inputParameters;
        this.outputParameters = outputParameters;
    }

    public List<String> getParents() {
        return parents;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getResult() {
        return result;
    }

    public String toString() {
        return String.join("/", parents) + ":" + result + (message != null ? "," + message : "");
    }

    public long getDuration() {
        return duration;
    }

    public List<UftResultStepParameter> getInputParameters() {
        return inputParameters;
    }

    public List<UftResultStepParameter> getOutputParameters() {
        return outputParameters;
    }
}
