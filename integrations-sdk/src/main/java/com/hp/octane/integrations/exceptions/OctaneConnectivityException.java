package com.hp.octane.integrations.exceptions;

public class OctaneConnectivityException extends ErrorCodeBasedException {

    public static final String AUTHORIZATION_FAILURE_MESSAGE = "Authorization failure";
    public static final String CONN_SHARED_SPACE_INVALID_MESSAGE = "Unable to connect the shared space";
    public static final String UNEXPECTED_FAILURE_MESSAGE = "Connectivity test failed with unexpected failure" ;
    public static final String UNSUPPORTED_SDK_VERSION_MESSAGE = "The plugin version is outdated and not supported with ALM Octane server. Please upgrade to newer version" ;

    public static final String AUTHENTICATION_FAILURE_KEY = "AUTHENTICATION_FAILURE";
    public static final String AUTHORIZATION_FAILURE_KEY = "AUTHORIZATION_FAILURE";
    public static final String CONN_SHARED_SPACE_INVALID_KEY = "CONNECTION_SHARED_SPACE_INVALID";
    public static final String UNEXPECTED_FAILURE_KEY = "UNEXPECTED_FAILURE";
    public static final String UNSUPPORTED_SDK_VERSION_KEY = "UNSUPPORTED_SDK_VERSION";

    private String errorMessageKey;
    private String errorMessageVal;

    public static final String AUTHENTICATION_FAILURE_MESSAGE = "Authentication failure. Check credentials or server location. If using proxy, check that your proxy settings are correct";
    public OctaneConnectivityException(int errorCode, String errorMessageKey, String errorMessageVal) {
        super(errorCode);

        this.errorMessageKey = errorMessageKey;
        this.errorMessageVal = errorMessageVal;
    }

    public String getErrorMessageKey() {
        return errorMessageKey;
    }

    public void setErrorMessageKey(String errorMessageKey) {
        this.errorMessageKey = errorMessageKey;
    }

    public String getErrorMessageVal() {
        return errorMessageVal;
    }

    public void setErrorMessageVal(String errorMessageVal) {
        this.errorMessageVal = errorMessageVal;
    }

}
