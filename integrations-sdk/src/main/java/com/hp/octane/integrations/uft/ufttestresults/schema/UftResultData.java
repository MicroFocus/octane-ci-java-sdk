package com.hp.octane.integrations.uft.ufttestresults.schema;

import java.util.List;

public class UftResultData {
    private List<String> parents;
    private String type;
    private String result;
    private String message;
    private long duration;

    public UftResultData(List<String> parents, String type, String result, String message, long duration) {
        this.parents = parents;
        this.type = type;
        this.result = result;
        this.message = message;
        this.duration = duration;
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
}
