package com.hp.octane.integrations.dto.executor;

import com.hp.octane.integrations.dto.DTOBase;

/**
 * Created by berkovir on 27/03/2017.
 */
public interface TestExecutionInfo extends DTOBase {

    String getTestName();

    TestExecutionInfo setTestName(String testName);

    String getPackageName();

    TestExecutionInfo setPackageName(String packageName);

}
