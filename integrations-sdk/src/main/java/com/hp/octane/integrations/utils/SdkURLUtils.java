package com.hp.octane.integrations.utils;

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.exceptions.OctaneSDKGeneralException;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class SdkURLUtils {

    private static final String PARAM_SHARED_SPACE = "p"; // NON-NLS
    private static final String UI_SPACE = "/ui";

    private static final String UNEXPECTED_SHARED_SPACE_EXCEPTION = "Unexpected shared space parameter value";
    private static final String APPLICATION_CONTEXT_NOT_FOUND_EXCEPTION = "Application context not found in URL";
    private static final String MISSING_SHARED_SPACE_EXCEPTION ="Missing shared space parameter";
    private static final String URL_INVALID_EXCEPTION ="Invalid URL";

    public static OctaneConfiguration parseUiLocation(String instanceId, String uiLocation) throws OctaneSDKGeneralException {
        try {
            URL url = new URL(uiLocation);
            String location;
            int contextPos = uiLocation.indexOf(UI_SPACE);
            if (contextPos < 0) {
                throw new OctaneSDKGeneralException(APPLICATION_CONTEXT_NOT_FOUND_EXCEPTION);

            } else {
                location = uiLocation.substring(0, contextPos);
            }
            List<NameValuePair> params = URLEncodedUtils.parse(url.toURI(), "UTF-8");
            for (NameValuePair param : params) {
                if (param.getName().equals(PARAM_SHARED_SPACE)) {
                    String[] sharedSpaceAndWorkspace = param.getValue().split("/");
                    // we are relaxed and allow parameter without workspace in order not to force user to makeup
                    // workspace value when configuring manually or via config API and not via copy & paste
                    if (sharedSpaceAndWorkspace.length < 1 || SdkStringUtils.isEmpty(sharedSpaceAndWorkspace[0])) {
                        throw new OctaneSDKGeneralException(UNEXPECTED_SHARED_SPACE_EXCEPTION);
                    }
                    return new OctaneConfiguration(instanceId,location,sharedSpaceAndWorkspace[0]);
                }
            }
            throw new OctaneSDKGeneralException(MISSING_SHARED_SPACE_EXCEPTION);
        } catch (MalformedURLException e) {
            throw new OctaneSDKGeneralException(URL_INVALID_EXCEPTION);
        } catch (URISyntaxException e) {
            throw new OctaneSDKGeneralException(URL_INVALID_EXCEPTION);
        }
    }
}
