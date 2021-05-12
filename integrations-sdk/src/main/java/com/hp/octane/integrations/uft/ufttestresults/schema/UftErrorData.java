package com.hp.octane.integrations.uft.ufttestresults.schema;

import java.util.List;

public class UftErrorData {
    private List<String> parents;
    private String type;
    private String result;
    private String message;

    public UftErrorData(List<String> parents, String type, String result, String message) {
        this.parents = parents;
        this.type = type;
        this.result = result;
        this.message = message;
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
}
