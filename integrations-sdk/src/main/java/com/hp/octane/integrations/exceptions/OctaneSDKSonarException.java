package com.hp.octane.integrations.exceptions;


public class OctaneSDKSonarException extends Exception {

    public OctaneSDKSonarException(Throwable throwable) {
        super(throwable);
    }

    public OctaneSDKSonarException(String message) {
        super(message);
    }

    public OctaneSDKSonarException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
