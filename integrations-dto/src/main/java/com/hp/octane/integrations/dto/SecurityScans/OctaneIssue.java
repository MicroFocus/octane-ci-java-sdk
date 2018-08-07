package com.hp.octane.integrations.dto.SecurityScans;

import java.util.Map;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.entities.Entity;

public interface OctaneIssue extends DTOBase {
    void set_extended_data(Map extendedData);
    void set_primary_location_full(String primaryLocationFull);
    void set_line(Integer line);
    void set_analysis(Entity analysis);
    void set_state(Entity state);
    void set_severity(Entity severity);
    Map get_extended_data();
    String get_primary_location_full();
    Integer get_line();
    Entity get_analysis();
    Entity set_state();
    Entity set_severity();
    String getRemote_id();
    void setRemote_id(String remote_id);
    String getIntroduced_date();
    void setIntroduced_date(String introducedDate);
    String getExternal_link();
    void setExternal_link(String external_link);
}
