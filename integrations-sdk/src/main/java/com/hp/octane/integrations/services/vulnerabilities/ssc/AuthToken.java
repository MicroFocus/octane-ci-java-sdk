package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by hijaziy on 7/23/2018.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthToken  extends SscBaseEntitySingle<AuthToken.AuthTokenData> {
//    {
//        "data": {
//        "terminalDate": "2017-09-12T11:24:28.000+0000",
//                "creationDate": "2017-09-11T11:24:28.000+0000",
//                "type": "UnifiedLoginToken",
//                "token": "OWUwZGJkNmEtODYzYS00ZTM4LWE1MGEtNWQxM2Y5NmRhYzg1"
//    },
//        "responseCode": 201
//    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthTokenData {
        @JsonProperty("token")
        public String token;
        @JsonProperty("terminalDate")
        public String terminalDate;
    }
}
