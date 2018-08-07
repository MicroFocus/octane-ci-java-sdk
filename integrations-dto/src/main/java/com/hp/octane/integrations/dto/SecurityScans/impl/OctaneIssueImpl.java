package com.hp.octane.integrations.dto.SecurityScans.impl;

import com.hp.octane.integrations.dto.SecurityScans.OctaneIssue;
import com.hp.octane.integrations.dto.entities.Entity;

import java.util.Map;

public class OctaneIssueImpl implements OctaneIssue {

    Map extented_data;
    String primaryLocation;
    Integer line;
    Entity analysis;
    Entity state;
    Entity severity;

    @Override
    public void set_extended_data(Map extendedData) {

    }

    @Override
    public void set_primary_location_full(String primaryLocationFull) {

    }

    @Override
    public void set_line(Integer line) {

    }

    @Override
    public void set_analysis(Entity analysis) {

    }

    @Override
    public void set_state(Entity state) {

    }

    @Override
    public void set_severity(Entity severity) {

    }

    @Override
    public Map get_extended_data() {
        return null;
    }

    @Override
    public String get_primary_location_full() {
        return null;
    }

    @Override
    public Integer get_line() {
        return null;
    }

    @Override
    public Entity get_analysis() {
        return null;
    }

    @Override
    public Entity set_state() {
        return null;
    }

    @Override
    public Entity set_severity() {
        return null;
    }
}
