package com.hp.octane.integrations.exceptions;

import com.hp.octane.integrations.dto.entities.OctaneBulkExceptionData;

public class OctaneBulkException extends RuntimeException {

    private OctaneBulkExceptionData data;
    private int responseStatus;

    public OctaneBulkException(int responseStatus, OctaneBulkExceptionData data) {
        super(data.getErrors().size() == 1
                ? data.getErrors().get(0).getDescription()
                : data.getErrors().size() + " exceptions occurred on Octane side.");
        this.data = data;
        this.responseStatus = responseStatus;
    }

    public OctaneBulkExceptionData getData() {
        return data;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

}
