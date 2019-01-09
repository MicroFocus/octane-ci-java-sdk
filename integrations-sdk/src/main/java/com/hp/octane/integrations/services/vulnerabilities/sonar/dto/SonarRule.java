package com.hp.octane.integrations.services.vulnerabilities.sonar.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarRule {

    private String key;
    private String name;
    private String createdAt;
    private String  htmlDesc;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getHtmlDesc() {
        return htmlDesc;
    }

    public void setHtmlDesc(String htmlDesc) {
        this.htmlDesc = htmlDesc;
    }
}
