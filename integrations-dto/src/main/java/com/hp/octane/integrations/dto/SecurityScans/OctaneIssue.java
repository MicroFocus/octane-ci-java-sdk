package com.hp.octane.integrations.dto.SecurityScans;

import java.util.Map;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.entities.Entity;

public interface OctaneIssue extends DTOBase {
    void setExtended_data(Map extendedData);
    void setPrimary_location_full(String primaryLocationFull);
    void setLine(Integer line);
    void setAnalysis(Entity analysis);
    void setState(Entity state);
    void setSeverity(Entity severity);
    Map getExtended_data();
    String getPrimary_location_full();
    Integer getLine();
    Entity getAnalysis();
    Entity getState();
    Entity getSeverity();
    String getRemote_id();
    void setRemote_id(String remote_id);
    String getIntroduced_date();
    void setIntroduced_date(String introducedDate);
    String getExternal_link();
    void setExternal_link(String external_link);
}
