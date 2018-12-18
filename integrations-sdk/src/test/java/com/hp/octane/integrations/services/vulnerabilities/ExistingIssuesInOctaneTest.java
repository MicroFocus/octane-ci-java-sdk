package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.services.vulnerabilities.Mocks.MockOctaneRestClient;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.hp.octane.integrations.services.vulnerabilities.SSCTestUtils.getJson;

public class ExistingIssuesInOctaneTest {
    @Test
    public void emptyList() throws IOException {
        ExistingIssuesInOctane existingIssuesInOctane = buildExistingIssuesInOctane("[]");
        List<String> remoteIdsOpenVulnsFromOctane = existingIssuesInOctane.getRemoteIdsOpenVulnsFromOctane("Job1", "1", "Tag1");
        Assert.assertEquals(0, remoteIdsOpenVulnsFromOctane.size());
    }


    private ExistingIssuesInOctane buildExistingIssuesInOctane(String s) {

        VulnerabilitiesServiceImpl.VulnerabilitiesQueueItem queueItem =
                new VulnerabilitiesServiceImpl.VulnerabilitiesQueueItem();
        queueItem.jobId = "ABC";
        queueItem.buildId = "1";
        return new ExistingIssuesInOctane(new MockOctaneRestClient(s, 200),
                new OctaneConfiguration("instanceID", "http://URL:8080", "1002"),
                queueItem);
    }

    @Test
    public void nonEmptyList() throws IOException {
        List<String> remoteIds = new ArrayList<>();
        remoteIds.add("Id1");
        remoteIds.add("Id2");
        remoteIds.add("Id3");

        String jsonVal = getJson(remoteIds);

        ExistingIssuesInOctane existingIssuesInOctane = buildExistingIssuesInOctane(jsonVal);
        List<String> remoteIdsOpenVulnsFromOctane = existingIssuesInOctane.getRemoteIdsOpenVulnsFromOctane("Job2", "2","Tag2");

        Assert.assertEquals("Id1",remoteIdsOpenVulnsFromOctane.get(0));
        Assert.assertEquals("Id2",remoteIdsOpenVulnsFromOctane.get(1));
        Assert.assertEquals("Id3",remoteIdsOpenVulnsFromOctane.get(2));
    }


}
