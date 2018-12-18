package com.hp.octane.integrations.services.vulnerabilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.vulnerabilities.PackIssuesToSendToOctane;
import com.hp.octane.integrations.services.vulnerabilities.ssc.Issues;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PackIssuesToSendToOctaneTest {
	private String targetDir;

	@Before
	public void prepareOutDir() {
		String currentDir = System.getProperty("user.dir");

		String targetDir = String.join(File.separator, currentDir, "temp", "SSCTests");
		File file = new File(targetDir);
		if (!file.exists()) {
			if (!file.mkdirs()) {
				throw new IllegalStateException("failed to create temporary folder for tests output");
			}
		}
		this.targetDir = targetDir;
	}

	@Test(expected = PermanentException.class)
	public void packBasicNoIssuesToClose() {
		PackIssuesToSendToOctane packIssuesToSendToOctane = new PackIssuesToSendToOctane();
		Issues issues = new Issues();
		issues.setData(new ArrayList<>());
		ArrayList<String> existingInOctane = new ArrayList<>();
		packIssuesToSendToOctane.packAllIssues(issues, existingInOctane, targetDir, "Tag");
	}

	@Test
	public void packIssuesToClose() throws IOException {
		PackIssuesToSendToOctane packIssuesToSendToOctane = new PackIssuesToSendToOctane();
		Issues issues = new Issues();
		issues.setData(new ArrayList<>());
		List<String> toCloseInOctane = Arrays.asList("Id1", "Id2");
		InputStream inputStream = packIssuesToSendToOctane.packAllIssues(issues, toCloseInOctane, targetDir, "Tag");

		List<Map> issuesToPost = getIssuesAsMaps(inputStream);
		Assert.assertEquals(2, issuesToPost.size());
		Map issueState1 = (Map) issuesToPost.get(0).get("state");
		Assert.assertEquals("list_node.issue_state_node.closed", issueState1.get("id"));
		Assert.assertEquals("list_node", issueState1.get("type"));
		Assert.assertEquals("Id1", issuesToPost.get(0).get("remote_id"));

		Map issueState2 = (Map) issuesToPost.get(1).get("state");
		Assert.assertEquals("list_node.issue_state_node.closed", issueState2.get("id"));
		Assert.assertEquals("list_node", issueState2.get("type"));
		Assert.assertEquals("Id2", issuesToPost.get(1).get("remote_id"));
	}

	@Test
	public void packIssuesToNewAndUpdate() throws IOException {

		Issues issues = sscIssuesToPack();

		PackIssuesToSendToOctane packIssuesToSendToOctane = new PackIssuesToSendToOctane();
		InputStream inputStream = packIssuesToSendToOctane.packAllIssues(issues, new ArrayList<>(), this.targetDir, "Tag");
		List<Map> issuesToPost = getIssuesAsMaps(inputStream);
		Assert.assertEquals(2, issuesToPost.size());


		Map issue1AsMap = issuesToPost.get(0);
		validateIssueMap(issue1AsMap,
				"list_node.issue_state_node.new",
				"\\ABC\\DEF\\GHIJ.java",
				1,
				"Kingdom1",
				"Issue1");

		Map issue2AsMap = issuesToPost.get(1);
		validateIssueMap(issue2AsMap,
				"list_node.issue_state_node.existing",
				"\\ABC\\DEF\\GHIJ\\KLM.java",
				2,
				"Kingdom2",
				"Issue2");
	}

	@Test
	public void packSomeToCloseAndSomeToNewAndUpdate() throws IOException {
		PackIssuesToSendToOctane packIssuesToSendToOctane = new PackIssuesToSendToOctane();
		List<String> existingInOctane = Arrays.asList("Id1", "Id2", "RemoteId2");
		Issues issues = sscIssuesToPack();

		InputStream inputStream = packIssuesToSendToOctane.packAllIssues(issues, existingInOctane, this.targetDir, "Tag");
		List<Map> issuesToPost = getIssuesAsMaps(inputStream);
		Assert.assertEquals(4, issuesToPost.size());
		Map issue1AsMap = issuesToPost.get(0);
		validateIssueMap(issue1AsMap,
				"list_node.issue_state_node.new",
				"\\ABC\\DEF\\GHIJ.java",
				1,
				"Kingdom1",
				"Issue1");

		Map issue2AsMap = issuesToPost.get(1);
		validateIssueMap(issue2AsMap,
				"list_node.issue_state_node.existing",
				"\\ABC\\DEF\\GHIJ\\KLM.java",
				2,
				"Kingdom2",
				"Issue2");


		Map issue3AsMap = issuesToPost.get(2);
		validateIssueMap(issue3AsMap,
				"list_node.issue_state_node.closed",
				null,
				-1,
				null,
				null);

		Map issue4AsMap = issuesToPost.get(3);
		validateIssueMap(issue4AsMap,
				"list_node.issue_state_node.closed",
				null,
				-1,
				null,
				null);
	}

	private List<Map> getIssuesAsMaps(InputStream inputStream) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		Map map = objectMapper.readValue(inputStream, Map.class);
		return (List<Map>) map.get("data");
	}

	private void validateIssueMap(Map issue2AsMap, String state, String location, int line,
	                              String kingdom,
	                              String issueName) {
		if (state != null) {
			Assert.assertEquals(state, ((Map) (issue2AsMap.get("state"))).get("id"));
		}
		if (location != null) {
			Assert.assertEquals(location, issue2AsMap.get("primary_location_full"));
		}
		if (line != -1) {
			Assert.assertEquals(line, issue2AsMap.get("line"));
		}
		if (kingdom != null) {
			Assert.assertEquals(kingdom, ((Map) (issue2AsMap.get("extended_data"))).get("kingdom"));
		}
		if (issueName != null) {
			Assert.assertEquals(issueName, ((Map) (issue2AsMap.get("extended_data"))).get("issueName"));
		}
	}

	private Issues sscIssuesToPack() {
		Issues.Issue issue1 = new Issues.Issue();
		issue1.scanStatus = "NEW";
		issue1.issueInstanceId = "RemoteId1";
		issue1.fullFileName = "\\ABC\\DEF\\GHIJ.java";
		issue1.lineNumber = 1;
		issue1.kingdom = "Kingdom1";
		issue1.issueName = "Issue1";

		Issues.Issue issue2 = new Issues.Issue();
		issue2.scanStatus = "UPDATED";
		issue2.issueInstanceId = "RemoteId2";
		issue2.fullFileName = "\\ABC\\DEF\\GHIJ\\KLM.java";
		issue2.lineNumber = 2;
		issue2.kingdom = "Kingdom2";
		issue2.issueName = "Issue2";
		issue1.issueInstanceId = "RemoteId2";

		Issues issues = new Issues();
		issues.setData(Arrays.asList(issue1, issue2));
		issues.setCount(2);
		return issues;
	}
}
