package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.hp.octane.integrations.dto.snapshots.CIBuildStatus;

import java.io.Serializable;


public interface CIBuildStatusInfo extends DTOBase, Serializable {

    CIBuildStatus getBuildStatus();

    String getBuildCiId();

    CIBuildResult getBuildResult();

    CIBuildStatusInfo setBuildStatus(CIBuildStatus status);

    CIBuildStatusInfo setBuildCiId(String buildCiId);

    CIBuildStatusInfo setResult(CIBuildResult result);

    String getJobCiId();

    CIBuildStatusInfo setJobCiId(String jobCiId);

    String getParamName();

    CIBuildStatusInfo setParamName(String fieldName);

    String getParamValue();

    CIBuildStatusInfo setParamValue(String fieldValue);

    String getExceptionMessage();

    CIBuildStatusInfo setExceptionMessage(String exceptionMessage);
}
