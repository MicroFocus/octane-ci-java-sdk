package com.hp.octane.integrations.dto.SecurityScans.impl;

import com.hp.octane.integrations.dto.SecurityScans.OctaneIssue;
import com.hp.octane.integrations.dto.entities.Entity;

import java.util.Map;

public class OctaneIssueImpl implements OctaneIssue {

    Map extented_data;
    String primary_location_full;
    Integer line;
    Entity analysis;
    Entity state;
    Entity severity;
    String remote_id;
    String introduced_date;
    String external_link;

    @Override
    public void set_extended_data(Map extendedData) {
        this.extented_data = extendedData;
    }

    @Override
    public void set_primary_location_full(String primaryLocationFull) {
        this.primary_location_full = primaryLocationFull;
    }

    @Override
    public void set_line(Integer line) {
        this.line = line;
    }

    @Override
    public void set_analysis(Entity analysis) {
        this.analysis = analysis;
    }

    @Override
    public void set_state(Entity state) {
        this.state = state;
    }

    @Override
    public void set_severity(Entity severity) {
        this.severity = severity;
    }

    @Override
    public Map get_extended_data() {
        return this.extented_data;
    }

    @Override
    public String get_primary_location_full() {
        return this.primary_location_full;
    }

    @Override
    public Integer get_line() {
        return this.line;
    }

    @Override
    public Entity get_analysis() {
        return analysis;
    }

    @Override
    public Entity set_state() {
        return state;
    }

    @Override
    public Entity set_severity() {
        return severity;
    }

    public String getRemote_id() {
        return remote_id;
    }

    public void setRemote_id(String remote_id) {
        this.remote_id = remote_id;
    }

    @Override
    public String getIntroduced_date() {
        return this.introduced_date;
    }

    @Override
    public void setIntroduced_date(String introducedDate) {
        this.introduced_date = introducedDate;
    }

    @Override
    public String getExternal_link() {
        return this.external_link;
    }

    @Override
    public void setExternal_link(String external_link) {
        this.external_link = external_link;
    }
}
