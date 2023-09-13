/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
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

import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.IssueDetails;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Issues;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.hp.octane.integrations.services.vulnerabilities.ssc.SSCToOctaneIssueUtil.createOctaneIssues;

public class PackIssuesToSendToOctaneTest {

	String targetDir;
	@Before
	public void prepareOutDir(){

		String currentDir = System.getProperty("user.dir");

		String targetDir = currentDir + File.separator + "SSCTests";
		File file = new File(targetDir);
		if(!file.exists()){
			file.mkdirs();
		}
		this.targetDir = targetDir;

	}

	@Test(expected = PermanentException.class)
	public void packBasicNoIssuesToClose(){

		Issues issues = new Issues();
		issues.setData(new ArrayList<>());
		ArrayList<String> existingInOctane = new ArrayList<>();
		PackIssuesToOctaneUtils.packToOctaneIssues(issues.getData(), existingInOctane,
				 true);

	}

	@Test
	public void packIssuesToClose() throws IOException {

		Issues issues = new Issues();
		issues.setData(new ArrayList<>());
		List<String> toCloseInOctane = Arrays.asList("Id1","Id2");
		PackIssuesToOctaneUtils.SortedIssues<Issues.Issue> issueSortedIssues = PackIssuesToOctaneUtils.packToOctaneIssues(issues.getData(),
				toCloseInOctane,  true);

		Assert.assertEquals(2, issueSortedIssues.issuesToClose.size());
		Entity issueState1 = issueSortedIssues.issuesToClose.get(0).getState();
		Assert.assertEquals("list_node.issue_state_node.closed", issueState1.getId());
		Assert.assertEquals("list_node", issueState1.getType());
		Assert.assertEquals("Id1", issueSortedIssues.issuesToClose.get(0).getRemoteId());

		Entity issueState2 = issueSortedIssues.issuesToClose.get(1).getState();
		Assert.assertEquals("list_node.issue_state_node.closed", issueState2.getId());
		Assert.assertEquals("list_node", issueState2.getType());
		Assert.assertEquals("Id2", issueSortedIssues.issuesToClose.get(1).getRemoteId());

	}

	@Test
	public void packIssuesToNewAndUpdate() throws IOException {

		Issues issues = sscIssuesToPack();
		Map<Integer,IssueDetails> idToDetails = getAllData();

		PackIssuesToOctaneUtils.SortedIssues<Issues.Issue> issueSortedIssues = PackIssuesToOctaneUtils.packToOctaneIssues(issues.getData(),
				new ArrayList<>(), true);
		Assert.assertEquals(2,issueSortedIssues.issuesToUpdate.size());
		Assert.assertEquals(2,issueSortedIssues.issuesRequiredExtendedData.size());
		Assert.assertEquals(0,issueSortedIssues.issuesToClose.size());

		List<OctaneIssue> openOctaneIssues = createOctaneIssues(issueSortedIssues.issuesToUpdate, "Tag",idToDetails);

		validateIssueMap(openOctaneIssues.get(0),
				"list_node.issue_state_node.new",
				"\\ABC\\DEF\\GHIJ.java",
				"1",
				"Kingdom1",
				"Issue1");


		validateIssueMap(openOctaneIssues.get(1),
				"list_node.issue_state_node.existing",
				"\\ABC\\DEF\\GHIJ\\KLM.java",
				"2",
				"Kingdom2",
				"Issue2");


		validateRemoteIdAndExtendedIssues(openOctaneIssues.get(0),"RemoteId1",idToDetails.get(1));
		validateRemoteIdAndExtendedIssues(openOctaneIssues.get(1),"RemoteId2",idToDetails.get(2));

	}

	private void validateRemoteIdAndExtendedIssues(OctaneIssue issue2AsMap, String remoteId1, IssueDetails issueDetails) {

		Map extended_data = (issue2AsMap.getExtendedData());
		Assert.assertEquals(issueDetails.getData().brief ,extended_data.get("summary"));
		Assert.assertEquals(issueDetails.getData().recommendation, extended_data.get("recommendations"));
		Assert.assertEquals(issueDetails.getData().tips, extended_data.get("tips"));
		Assert.assertEquals(issueDetails.getData().detail, extended_data.get("explanation"));

		if(remoteId1 != null) {
			Assert.assertEquals(remoteId1, issue2AsMap.getRemoteId());
		}
	}


	@Test
	public void packSomeToCloseAndSomeToNewAndUpdate() throws IOException {

		List<String> existingInOctane = Arrays.asList("XYZ","LMNO","RemoteId2");
		Issues issues = sscIssuesToPack();

		PackIssuesToOctaneUtils.SortedIssues<Issues.Issue> issueSortedIssues = PackIssuesToOctaneUtils.packToOctaneIssues(issues.getData(),
				existingInOctane, true);

		List<OctaneIssue> octaneIssues = createOctaneIssues(issueSortedIssues.issuesToUpdate,"Tag", new HashMap<>());

		Assert.assertEquals(2,octaneIssues.size());
		Assert.assertEquals(2,issueSortedIssues.issuesToClose.size());

		validateIssueMap(octaneIssues.get(0),
				"list_node.issue_state_node.new",
				"\\ABC\\DEF\\GHIJ.java",
				"1",
				"Kingdom1",
				"Issue1");


		validateIssueMap(octaneIssues.get(1),
				"list_node.issue_state_node.existing",
				"\\ABC\\DEF\\GHIJ\\KLM.java",
				"2",
				"Kingdom2",
				"Issue2");



		validateIssueMap(issueSortedIssues.issuesToClose.get(0),
				"list_node.issue_state_node.closed",
				null,
				"-1",
				null,
				null);
		Assert.assertEquals(issueSortedIssues.issuesToClose.get(0).getRemoteId(), "XYZ");


		validateIssueMap(issueSortedIssues.issuesToClose.get(1),
				"list_node.issue_state_node.closed",
				null,
				"-1",
				null,
				null);
		Assert.assertEquals(issueSortedIssues.issuesToClose.get(1).getRemoteId(), "LMNO");

	}
	private Map<Integer, IssueDetails> getAllData() {
		IssueDetails issueDetails1 = new IssueDetails();
		issueDetails1.setData(new IssueDetails.IssueDetailsData());
		issueDetails1.getData().brief = "summary1";
		issueDetails1.getData().detail = "explanation1";
		issueDetails1.getData().recommendation = "recommendations1";
		issueDetails1.getData().tips = "tips1";


		IssueDetails issueDetails2 = new IssueDetails();
		issueDetails2.setData(new IssueDetails.IssueDetailsData());
		issueDetails2.getData().brief = "summary2";
		issueDetails2.getData().detail = "explanation2";
		issueDetails2.getData().recommendation = "recommendations2";
		issueDetails2.getData().tips = "tips2";

		HashMap<Integer,IssueDetails> retVal = new HashMap<>();
		retVal.put(1,issueDetails1);
		retVal.put(2,issueDetails2);
		return retVal;
	}


	private void validateIssueMap(OctaneIssue octaneIssue, String state, String location, String line,
								  String kingdom,
								  String issueName) {
		if(state != null) {
			Assert.assertEquals(state, octaneIssue.getState().getId());
		}
		if(location != null) {
			Assert.assertEquals(location, octaneIssue.getPrimaryLocationFull());
		}
		if(!"-1".equals(line)) {
			Assert.assertEquals(line, octaneIssue.getLine().toString());
		}
		if(kingdom != null) {
			Assert.assertEquals(kingdom, octaneIssue.getExtendedData().get("kingdom"));
		}
		if(issueName != null) {
			Assert.assertEquals(issueName, octaneIssue.getExtendedData().get("issueName"));
		}
	}

	private Issues sscIssuesToPack() {
		Issues.Issue issue1 = new Issues.Issue();
		issue1.id = 1;
		issue1.scanStatus = "NEW";
		issue1.issueInstanceId = "RemoteId1";
		issue1.fullFileName = "\\ABC\\DEF\\GHIJ.java";
		issue1.lineNumber = 1;
		issue1.kingdom = "Kingdom1";
		issue1.issueName = "Issue1";
		issue1.issueInstanceId = "RemoteId1";


		Issues.Issue issue2 = new Issues.Issue();
		issue2.id = 2;
		issue2.scanStatus = "UPDATED";
		issue2.issueInstanceId = "RemoteId2";
		issue2.fullFileName = "\\ABC\\DEF\\GHIJ\\KLM.java";
		issue2.lineNumber = 2;
		issue2.kingdom = "Kingdom2";
		issue2.issueName = "Issue2";
		issue2.issueInstanceId = "RemoteId2";

		Issues issues = new Issues();
		issues.setData(Arrays.asList(issue1, issue2));
		issues.setCount(2);
		return issues;
	}
}
