package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOBase;

import java.util.List;

public interface MbtDataTable extends DTOBase {

    List<String> getParameters();

    MbtDataTable setParameters(List<String> parameters);

    List<List<String>> getIterations();

    MbtDataTable setIterations(List<List<String>> iterations);
}
