package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.vulnerabilities.ssc.Issues;
import com.hp.octane.integrations.services.vulnerabilities.Mocks.DummyContents;
import com.hp.octane.integrations.services.vulnerabilities.Mocks.MockSSCRestClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import static com.hp.octane.integrations.services.vulnerabilities.SSCTestUtils.*;
import static org.easymock.EasyMock.*;

public class SSCHandlerTest {

    VulnerabilitiesServiceImpl.VulnerabilitiesQueueItem queueItem;
    SSCProjectConfiguration configMock;

    @Before
    public void prepareMembers(){
        queueItem = new
                VulnerabilitiesServiceImpl.VulnerabilitiesQueueItem();
        queueItem.startTime = System.currentTimeMillis() - 1000*60*60; //An hour ago

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
