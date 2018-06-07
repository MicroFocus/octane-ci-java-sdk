package com.hp.octane.integrations.exceptions;

import com.hp.octane.integrations.dto.entities.OctaneBulkExceptionData;

public class OctaneBulkException extends RuntimeException {

    private OctaneBulkExceptionData data;
    private int responseStatus;

    public OctaneBulkException(int responseStatus, OctaneBulkExceptionData data){
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
