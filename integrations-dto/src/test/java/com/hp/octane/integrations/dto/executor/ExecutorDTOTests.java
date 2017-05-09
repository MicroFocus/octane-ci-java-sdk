/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.octane.integrations.dto.executor;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.dto.scm.SCMType;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gullery on 03/01/2016.
 */

public class ExecutorDTOTests {
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();

    @Test
    public void testDiscoveryInfo() {

        SCMRepository scm = dtoFactory.newDTO(SCMRepository.class);
        scm.setType(SCMType.GIT);
        scm.setUrl("git:bubu");

        DiscoveryInfo discInfo = dtoFactory.newDTO(DiscoveryInfo.class);
        discInfo
                .setExecutorId("123")
                .setWorkspaceId("789")
                .setForceFullDiscovery(true)
                .setScmRepository(scm);

        String json = dtoFactory.dtoToJson(discInfo);
        Assert.assertNotNull(json);
    }

    @Test
    public void testTestExecutionInfo() throws IOException {

        SCMRepository scm = dtoFactory.newDTO(SCMRepository.class);
        scm.setType(SCMType.GIT);
        scm.setUrl("git:bubu");

        TestExecutionInfo testExecInfo1 = dtoFactory.newDTO(TestExecutionInfo.class);
        testExecInfo1
                .setPackageName("pac1")
                .setTestName("test1");

        TestExecutionInfo testExecInfo2 = dtoFactory.newDTO(TestExecutionInfo.class);
        testExecInfo2
                .setPackageName("pac2")
                .setTestName("test2");
        List<TestExecutionInfo> tests = new ArrayList<>();
        tests.add(testExecInfo1);
        tests.add(testExecInfo2);

        TestSuiteExecutionInfo testSuiteInfo = dtoFactory.newDTO(TestSuiteExecutionInfo.class);
        testSuiteInfo
                .setTestingToolType(TestingToolType.UFT)
                .setWorkspaceId("789")
                .setScmRepository(scm)
                .setSuiteId("123")
                .setTests(tests);


        String json = dtoFactory.dtoToJson(testSuiteInfo);
        Assert.assertNotNull(json);
    }
}
