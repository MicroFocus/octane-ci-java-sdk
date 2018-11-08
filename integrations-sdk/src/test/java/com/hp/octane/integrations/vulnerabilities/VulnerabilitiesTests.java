package com.hp.octane.integrations.vulnerabilities;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.services.rest.SSCRestClient;
import com.hp.octane.integrations.services.vulnerabilities.SSCHandler;
import com.hp.octane.integrations.services.vulnerabilities.ssc.Issues;
import com.hp.octane.integrations.services.vulnerabilities.ssc.SscProjectConnector;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.easymock.EasyMock.*;

public class VulnerabilitiesTests {
    private DTOFactory dtoFactory = DTOFactory.getInstance();

    @Test
    public void wellFormedURLS() {
        SSCProjectConfiguration sscFortifyConfigurations = dtoFactory.newDTO(SSCProjectConfiguration.class)
                .setSSCUrl("server_url")
                .setProjectName("project")
                .setProjectVersion("version")
                .setSSCBaseAuthToken("");

        SSCRestClient sscClientMock = createNiceMock(SSCRestClient.class);
        replay();
        SscProjectConnector sscProjectConnector = new SscProjectConnector(sscFortifyConfigurations, sscClientMock);

        String projectIdURL = sscProjectConnector.getProjectIdURL();
        String newIssuesURL = sscProjectConnector.getNewIssuesURL(1);
        String artifactsURL = sscProjectConnector.getArtifactsURL(100, 1000);
        String urlForProjectVersion = sscProjectConnector.getURLForProjectVersion(500);

        Assert.assertEquals(projectIdURL, "projects?q=name:project");
        Assert.assertEquals(newIssuesURL, "projectVersions/1/issues?q=[issue_age]:new&qm=issues&showhidden=false&showremoved=false&showsuppressed=false");
        Assert.assertEquals(artifactsURL, "projectVersions/100/artifacts?limit=1000");
        Assert.assertEquals(urlForProjectVersion, "projects/500/versions?q=name:version");
    }

    @Test
    public void analysisSSCToOctaneWellTransformed() {
        Issues sscIssues = new Issues();
        Issues.Issue issue1 = new Issues.Issue();
        issue1.reviewed = true;

        Issues.Issue issue2 = new Issues.Issue();
        issue2.audited = true;

        Issues.Issue issue3 = new Issues.Issue();
        issue3.issueStatus = "reviewed";

        Issues.Issue issue4 = new Issues.Issue();

        sscIssues.setData(Arrays.asList(issue1, issue2, issue3, issue4));

        SSCHandler sscHandler = new SSCHandler();
        List<OctaneIssue> octaneIssues = sscHandler.createOctaneIssues(sscIssues);
        for (int i = 0; i < 4; i++) {
            if (i != 3) {
                Assert.assertEquals("list_node.issue_analysis_node.reviewed", octaneIssues.get(i).getAnalysis().getId());
            } else {
                Assert.assertNull(octaneIssues.get(i).getAnalysis());
            }
        }
    }

    @Test
    public void stateOctaneWellTransformed() {
        Issues.Issue issue1 = new Issues.Issue();
        issue1.scanStatus = "UPDATED";
        Issues.Issue issue2 = new Issues.Issue();
        issue2.scanStatus = "NEW";
        Issues.Issue issue3 = new Issues.Issue();
        issue3.scanStatus = "REINTRODUCED";
        Issues.Issue issue4 = new Issues.Issue();
        issue4.scanStatus = "REMOVED";
        Issues.Issue issue5 = new Issues.Issue();

        Issues sscIssues = new Issues();
        sscIssues.setData(Arrays.asList(issue1, issue2, issue3, issue4, issue5));

        SSCHandler sscHandler = new SSCHandler();
        List<OctaneIssue> octaneIssues = sscHandler.createOctaneIssues(sscIssues);

        String[] expectedValues = new String[]{
                "list_node.issue_state_node.existing",
                "list_node.issue_state_node.new", "list_node.issue_state_node.reopen",
                "list_node.issue_state_node.closed"
        };

        for (int i = 0; i < 5; i++) {
            if (i != 4) {
                Assert.assertEquals(expectedValues[i], octaneIssues.get(i).getState().getId());
            } else {
                Assert.assertNull(octaneIssues.get(i).getState());
            }
        }
    }

    @Test
    public void extendedData() {
        Issues.Issue issue = new Issues.Issue();
        issue.issueName = "name";
        issue.likelihood = "likelihood";
        issue.impact = "impact";
        issue.kingdom = "kingdom";
        issue.confidance = "confidence";
        issue.removedDate = "removedDate";

        Issues sscIssues = new Issues();
        sscIssues.setData(Arrays.asList(issue));
        SSCHandler sscHandler = new SSCHandler();
        List<OctaneIssue> octaneIssues = sscHandler.createOctaneIssues(sscIssues);

        Assert.assertEquals(octaneIssues.get(0).getExtendedData().get("issueName"), "name");
        Assert.assertEquals(octaneIssues.get(0).getExtendedData().get("likelihood"), "likelihood");
        Assert.assertEquals(octaneIssues.get(0).getExtendedData().get("kingdom"), "kingdom");
        Assert.assertEquals(octaneIssues.get(0).getExtendedData().get("impact"), "impact");
        Assert.assertEquals(octaneIssues.get(0).getExtendedData().get("confidence"), "confidence");
        Assert.assertEquals(octaneIssues.get(0).getExtendedData().get("removedDate"), "removedDate");
    }

    @Test
    public void simpleFields() {
        Issues.Issue issue = new Issues.Issue();
        issue.fullFileName = "fullFileName";
        issue.lineNumber = 100;
        issue.issueInstanceId = "ID_ID_ID";
        issue.foundDate = "2018-09-12T14:01:20.590+0000";
        issue.hRef = "hRef";

        Issues sscIssues = new Issues();
        sscIssues.setData(Arrays.asList(issue));
        SSCHandler sscHandler = new SSCHandler();
        List<OctaneIssue> octaneIssues = sscHandler.createOctaneIssues(sscIssues);

        Assert.assertEquals(octaneIssues.get(0).getPrimaryLocationFull(), "fullFileName");
        Assert.assertEquals(String.valueOf(octaneIssues.get(0).getLine()), String.valueOf(100));
        Assert.assertEquals(octaneIssues.get(0).getRemoteId(), "ID_ID_ID");
        Assert.assertNotNull(octaneIssues.get(0).getIntroducedDate());
        Assert.assertEquals(octaneIssues.get(0).getExternalLink(), "hRef");
    }

    @Test
    public void deserializeIssues(){

        Issues issues = SscProjectConnector.stringToObject(sampleSSCIssues, Issues.class);
        Assert.assertEquals(1,issues.getCount());
        Assert.assertEquals(1,issues.getData().size());
        Assert.assertEquals("pom.xml",issues.getData().get(0).fullFileName);

    }
    private final String sampleSSCIssues = "{\n"+
            "  \"data\": [\n"+
            "    {\n"+
            "      \"bugURL\": null,\n"+
            "      \"hidden\": false,\n"+
            "      \"issueName\": \"Build Misconfiguration: External Maven Dependency Repository\",\n"+
            "      \"folderGuid\": \"bb824e8d-b401-40be-13bd-5d156696a685\",\n"+
            "      \"lastScanId\": 155,\n"+
            "      \"engineType\": \"SCA\",\n"+
            "      \"issueStatus\": \"Unreviewed\",\n"+
            "      \"friority\": \"Low\",\n"+
            "      \"analyzer\": \"Configuration\",\n"+
            "      \"primaryLocation\": \"pom.xml\",\n"+
            "      \"reviewed\": null,\n"+
            "      \"id\": 3708,\n"+
            "      \"suppressed\": false,\n"+
            "      \"hasAttachments\": false,\n"+
            "      \"engineCategory\": \"STATIC\",\n"+
            "      \"projectVersionName\": null,\n"+
            "      \"removedDate\": null,\n"+
            "      \"severity\": 2.0,\n"+
            "      \"_href\": \"http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/116/issues/3708\",\n"+
            "      \"displayEngineType\": \"SCA\",\n"+
            "      \"foundDate\": \"2018-10-09T07:43:16.000+0000\",\n"+
            "      \"confidence\": 5.0,\n"+
            "      \"impact\": 2.0,\n"+
            "      \"primaryRuleGuid\": \"FF57412F-DD28-44DE-8F4F-0AD39620768C\",\n"+
            "      \"projectVersionId\": 116,\n"+
            "      \"scanStatus\": \"UPDATED\",\n"+
            "      \"audited\": false,\n"+
            "      \"kingdom\": \"Environment\",\n"+
            "      \"folderId\": 215,\n"+
            "      \"revision\": 0,\n"+
            "      \"likelihood\": 0.8,\n"+
            "      \"removed\": false,\n"+
            "      \"issueInstanceId\": \"87E3EC5CC8154C006783CC461A6DDEEB\",\n"+
            "      \"hasCorrelatedIssues\": false,\n"+
            "      \"primaryTag\": null,\n"+
            "      \"lineNumber\": 3,\n"+
            "      \"projectName\": null,\n"+
            "      \"fullFileName\": \"pom.xml\",\n"+
            "      \"primaryTagValueAutoApplied\": false\n"+
            "    }\n"+
            "  ],\n"+
            "  \"count\": 1,\n"+
            "  \"responseCode\": 200,\n"+
            "  \"links\": {\n"+
            "    \"last\": {\n"+
            "      \"href\": \"http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/116/issues?q=[issue_age]:updated&qm=issues&showhidden=false&showremoved=false&showsuppressed=false&start=0\"\n"+
            "    },\n"+
            "    \"first\": {\n"+
            "      \"href\": \"http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/116/issues?q=[issue_age]:updated&qm=issues&showhidden=false&showremoved=false&showsuppressed=false&start=0\"\n"+
            "    }\n"+
            "  }\n"+
            "}";
}
