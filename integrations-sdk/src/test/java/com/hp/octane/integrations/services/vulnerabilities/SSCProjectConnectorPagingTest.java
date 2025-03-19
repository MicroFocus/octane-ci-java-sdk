/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.services.vulnerabilities.ssc.SSCProjectConnector;
import com.hp.octane.integrations.services.vulnerabilities.mocks.DummyContents;
import com.hp.octane.integrations.services.vulnerabilities.mocks.MockSSCRestClient;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Issues;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.easymock.EasyMock.*;

public class SSCProjectConnectorPagingTest {


    @Test
    public void pagingOfIssues(){

        SSCProjectConfiguration configMock = createNiceMock(SSCProjectConfiguration.class);
        expect(configMock.getSSCUrl()).andReturn("DummyURL").anyTimes();
        replay(configMock);
        SSCProjectConnector sscProjectConnector = new SSCProjectConnector(configMock,
                new MockSSCRestClient(Arrays.asList(DummyContents.issuesPart1,
                        DummyContents.issuesPart2,
                        DummyContents.issuesPart3)));
        Issues issues = sscProjectConnector.readIssues(1);
        Assert.assertEquals(3,issues.getCount());
        Assert.assertEquals("Issue 1",issues.getData().get(0).issueName);
        Assert.assertEquals("Issue 2",issues.getData().get(1).issueName);
        Assert.assertEquals("Issue 3",issues.getData().get(2).issueName);

    }

}
