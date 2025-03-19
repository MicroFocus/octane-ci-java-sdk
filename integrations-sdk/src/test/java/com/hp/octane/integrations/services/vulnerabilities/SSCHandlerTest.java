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
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.vulnerabilities.ssc.SSCHandler;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Issues;
import com.hp.octane.integrations.services.vulnerabilities.mocks.DummyContents;
import com.hp.octane.integrations.services.vulnerabilities.mocks.MockSSCRestClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import static com.hp.octane.integrations.services.vulnerabilities.SSCTestUtils.*;
import static org.easymock.EasyMock.*;

public class SSCHandlerTest {

    VulnerabilitiesQueueItem queueItem;
    SSCProjectConfiguration configMock;

    @Before
    public void prepareMembers(){
        queueItem = new
                VulnerabilitiesQueueItem();
        queueItem.setStartTime(System.currentTimeMillis() - 1000*60*60); //An hour ago

        configMock = getSscProjectConfiguration();

    }
    @Test
    public void scanIsNotOverTest() throws IOException {

        String projectResponse = getDummyProjectResponse();
        String projectVersionsResponse = getProjectVersionResponse();
        String artifactResponse = getArtificatResponse("PROCESSING");

        MockSSCRestClient mockSSCRestClient = new MockSSCRestClient(Arrays.asList(projectResponse,
                projectVersionsResponse,
                artifactResponse));

        SSCHandler sscHandler = new SSCHandler(queueItem, configMock, mockSSCRestClient);
        Assert.assertFalse(sscHandler.isScanProcessFinished());
    }

    @Test
    public void scanIsOverButNoIssueTest() throws IOException {

        String projectResponse = getDummyProjectResponse();
        String projectVersionsResponse = getProjectVersionResponse();
        String artifactResponse = getArtificatResponse("PROCESS_COMPLETE");

        MockSSCRestClient mockSSCRestClient = new MockSSCRestClient(Arrays.asList(projectResponse,
                projectVersionsResponse,
                artifactResponse,
                "{\"data\":[]}"));

        SSCHandler sscHandler = new SSCHandler(queueItem, configMock, mockSSCRestClient);
        Optional<Issues> issuesIfScanCompleted = sscHandler.getIssuesIfScanCompleted();
        Assert.assertTrue(issuesIfScanCompleted.isPresent());
        Assert.assertEquals(0, issuesIfScanCompleted.get().getData().size());

    }

    @Test(expected = PermanentException.class)
    public void errorInScanTest() throws IOException {

        String projectResponse = getDummyProjectResponse();
        String projectVersionsResponse = getProjectVersionResponse();
        String artifactResponse = getArtificatResponse("ERROR_PROCESSING");

        MockSSCRestClient mockSSCRestClient = new MockSSCRestClient(Arrays.asList(projectResponse,
                projectVersionsResponse,
                artifactResponse));

        SSCHandler sscHandler = new SSCHandler(queueItem, configMock, mockSSCRestClient);
        sscHandler.isScanProcessFinished();
    }

    @Test
    public void scanIsOverAndThereAreIssuesTest() throws IOException {

        String projectResponse = getDummyProjectResponse();
        String projectVersionsResponse = getProjectVersionResponse();
        String artifactResponse = getArtificatResponse("PROCESS_COMPLETE");

        MockSSCRestClient mockSSCRestClient = new MockSSCRestClient(Arrays.asList(projectResponse,
                projectVersionsResponse,
                artifactResponse,
                DummyContents.issuesPart1,
                DummyContents.issuesPart2,
                DummyContents.issuesPart3));

        SSCHandler sscHandler = new SSCHandler(queueItem, configMock, mockSSCRestClient);
        Optional<Issues> issuesIfScanCompleted = sscHandler.getIssuesIfScanCompleted();
        Assert.assertTrue(issuesIfScanCompleted.isPresent());
        Assert.assertEquals(3, issuesIfScanCompleted.get().getData().size());
    }



    private SSCProjectConfiguration getSscProjectConfiguration() {
        SSCProjectConfiguration configMock = createNiceMock(SSCProjectConfiguration.class);
        expect(configMock.getSSCUrl()).andReturn("DummyURL").anyTimes();
        expect(configMock.isValid()).andReturn(true).anyTimes();
        expect(configMock.getProjectName()).andReturn("ABCDEF").anyTimes();
        expect(configMock.getProjectVersion()).andReturn("1").anyTimes();


        replay(configMock);
        return configMock;
    }
}
