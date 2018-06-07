package com.hp.octane.integrations.exceptions;

import com.hp.octane.integrations.dto.entities.OctaneBulkExceptionData;

public class OctaneBulkException extends RuntimeException {

    private OctaneBulkExceptionData data;

    public OctaneBulkException(OctaneBulkExceptionData data){
        this.data = data;
    }

    public OctaneBulkExceptionData getData() {
        return data;
    }
}
