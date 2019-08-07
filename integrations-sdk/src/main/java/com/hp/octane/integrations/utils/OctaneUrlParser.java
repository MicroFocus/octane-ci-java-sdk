/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.hp.octane.integrations.utils;

import com.hp.octane.integrations.exceptions.OctaneSDKGeneralException;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class OctaneUrlParser {

    private final String location;
    private final String sharedSpace;

    private static final String PARAM_SHARED_SPACE = "p"; // NON-NLS
    private static final String UI_SPACE = "/ui";

    private static final String UNEXPECTED_SHARED_SPACE_EXCEPTION = "Unexpected shared space parameter value";
    private static final String APPLICATION_CONTEXT_NOT_FOUND_EXCEPTION = "Application context not found in URL";
    private static final String MISSING_SHARED_SPACE_EXCEPTION = "Missing shared space parameter";
    private static final String URL_INVALID_EXCEPTION = "Invalid URL";


    public OctaneUrlParser(String location, String sharedSpace) {
        this.location = location;
        this.sharedSpace = sharedSpace;
    }

    public String getLocation() {
        return location;
    }

    public String getSharedSpace() {
        return sharedSpace;
    }


    public static OctaneUrlParser parse(String uiLocation) throws OctaneSDKGeneralException {
        try {

            //move all values after the #.
            String myUiLocation = uiLocation;
            int anchorPart = uiLocation.indexOf("#");
            if (anchorPart > 0) {
                myUiLocation = uiLocation.substring(0, anchorPart);
            }

            URL url = new URL(myUiLocation);
            String location;
            int contextPos = myUiLocation.indexOf(UI_SPACE);
            if (contextPos < 0) {
                throw new OctaneSDKGeneralException(APPLICATION_CONTEXT_NOT_FOUND_EXCEPTION);

            } else {
                location = myUiLocation.substring(0, contextPos);
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
                    return new OctaneUrlParser(location, sharedSpaceAndWorkspace[0]);
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
