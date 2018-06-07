package com.hp.octane.integrations.exceptions;

import com.hp.octane.integrations.dto.entities.OctaneRestExceptionData;

public class OctaneRestException extends RuntimeException {
    private OctaneRestExceptionData data;

    public OctaneRestException(OctaneRestExceptionData data){
        this.data = data;
    }

    public OctaneRestExceptionData getData() {
        return data;
    }
}
