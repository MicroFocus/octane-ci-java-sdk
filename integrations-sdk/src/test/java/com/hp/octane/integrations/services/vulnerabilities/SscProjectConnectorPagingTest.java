package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.services.vulnerabilities.ssc.Issues;
import com.hp.octane.integrations.services.vulnerabilities.ssc.SscProjectConnector;
import com.hp.octane.integrations.services.vulnerabilities.Mocks.DummyContents;
import com.hp.octane.integrations.services.vulnerabilities.Mocks.MockSSCRestClient;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.easymock.EasyMock.*;

public class SscProjectConnectorPagingTest {


    @Test
    public void pagingOfIssues(){

        SSCProjectConfiguration configMock = createNiceMock(SSCProjectConfiguration.class);
        expect(configMock.getSSCUrl()).andReturn("DummyURL").anyTimes();
        replay(configMock);
        SscProjectConnector sscProjectConnector = new SscProjectConnector(configMock,
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
