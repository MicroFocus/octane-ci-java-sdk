package com.hp.octane.integrations.services.vulnerabilities.fod.dto.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.FODEntityCollection;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    @JsonProperty(value = "userId")
    public int userId;
    @JsonProperty(value = "userName")
    public String userName;
    @JsonProperty(value = "firstName")
    public String firstName;
    @JsonProperty(value = "lastName")
    public String lastName;
    @JsonProperty(value = "email")
    public String email;

    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class Users extends FODEntityCollection<User> {

    }
}
