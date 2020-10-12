/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
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
 */

package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneConfigurationIntern;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.exceptions.OctaneSDKGeneralException;
import com.hp.octane.integrations.services.vulnerabilities.ssc.*;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Issues;
import com.hp.octane.integrations.testhelpers.GeneralTestUtils;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;
import com.hp.octane.integrations.testhelpers.SSCServerSimulator;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Octane SDK functional sanity test
 *
 * Basic functional sanity test, configuring 2 clients against 2 Octane shared spaces (servers)
 * Validating connectivity, events sent and tests / logs push (including pre-flights) - all this in isolated non-interfering fashion
 */

public class VulnerabilitiesServiceFunctionalityTest {
    private static final Logger logger = LogManager.getLogger(VulnerabilitiesServiceFunctionalityTest.class);

    @Test(timeout = 20000)
    public void testVulnerabilitiesFunctional() {
        Map<String, OctaneSPEndpointSimulator> simulators = null;

        try {
            String spIdA = UUID.randomUUID().toString();
            String spIdB = UUID.randomUUID().toString();
            String clientAInstanceId = UUID.randomUUID().toString();
            String clientBInstanceId = UUID.randomUUID().toString();
            Map<String, List<String>> preflightRequestCollectors = new LinkedHashMap<>();
            Map<String, List<String>> pushVulnerabilitiesCollectors = new LinkedHashMap<>();

            //  init 2 shared space endpoints simulators
            simulators = initSPEPSimulators(
                    Stream.of(spIdA, spIdB).collect(Collectors.toSet()),
                    preflightRequestCollectors,
                    pushVulnerabilitiesCollectors);

            //
            //  I
            //  add one client and verify it works okay
            //
            OctaneConfiguration configA = new OctaneConfigurationIntern(clientAInstanceId, OctaneSPEndpointSimulator.getSimulatorUrl(), spIdA);
            OctaneClient clientA = OctaneSDK.addClient(configA, VulnerabilitiesServicePluginServicesTest.class);
            VulnerabilitiesService vulnerabilitiesServiceA = clientA.getVulnerabilitiesService();
            Assert.assertFalse(preflightRequestCollectors.containsKey(spIdA));
            Assert.assertFalse(preflightRequestCollectors.containsKey(spIdB));

            //
            //  II
            //  add one more client and verify they are both works okay
            //
            OctaneConfiguration configB = new OctaneConfigurationIntern(clientBInstanceId, OctaneSPEndpointSimulator.getSimulatorUrl(), spIdB);
            OctaneClient clientB = OctaneSDK.addClient(configB, VulnerabilitiesServicePluginServicesTest.class);
            VulnerabilitiesService vulnerabilitiesServiceB = clientB.getVulnerabilitiesService();

            vulnerabilitiesServiceA.enqueueRetrieveAndPushVulnerabilities("job-preflight-true", "1", ToolType.SSC, System.currentTimeMillis(), 1,null, null);
            vulnerabilitiesServiceA.enqueueRetrieveAndPushVulnerabilities("job-preflight-false", "1",ToolType.SSC, System.currentTimeMillis(), 1,null, null);
            vulnerabilitiesServiceB.enqueueRetrieveAndPushVulnerabilities("job-preflight-true", "1", ToolType.SSC, System.currentTimeMillis(), 1, null, null);
            vulnerabilitiesServiceB.enqueueRetrieveAndPushVulnerabilities("job-preflight-false", "1", ToolType.SSC, System.currentTimeMillis(), 1, null, null);
            GeneralTestUtils.waitAtMostFor(12000, () -> {
                if (preflightRequestCollectors.get(spIdA) != null && preflightRequestCollectors.get(spIdA).size() == 2 &&
                        preflightRequestCollectors.get(spIdB) != null && preflightRequestCollectors.get(spIdB).size() == 2) {
                    return true;
                } else {
                    return null;
                }
            });
            Assert.assertEquals(clientAInstanceId + "|job-preflight-true|1", preflightRequestCollectors.get(spIdA).get(0));
            Assert.assertEquals(clientAInstanceId + "|job-preflight-false|1", preflightRequestCollectors.get(spIdA).get(1));
            Assert.assertEquals(clientBInstanceId + "|job-preflight-true|1", preflightRequestCollectors.get(spIdB).get(0));
            Assert.assertEquals(clientBInstanceId + "|job-preflight-false|1", preflightRequestCollectors.get(spIdB).get(1));

            //
            //  III
            //  remove one client and verify it is shut indeed and the second continue to work okay
            //
            OctaneSDK.removeClient(clientA);

            //
            //  IV
            //  remove second client and ensure no interactions anymore
            //
            OctaneSDK.removeClient(clientB);

        } finally {
            //  remove clients
            OctaneSDK.getClients().forEach(OctaneSDK::removeClient);

            //  remove simulators
            if (simulators != null) removeSPEPSimulators(simulators.values());
            SSCServerSimulator.instance().endSimulation();
        }
    }

    @Test(timeout = 20000)
    public void testVulnerabilitiesFunctionalSSC() {
        Map<String, OctaneSPEndpointSimulator> simulators = null;

        try {
            String spIdA = UUID.randomUUID().toString();

            String clientAInstanceId = UUID.randomUUID().toString();
            Map<String, List<String>> preflightRequestCollectors = new LinkedHashMap<>();
            Map<String, List<String>> pushVulnerabilitiesCollectors = new LinkedHashMap<>();


            simulators = initSPEPSimulatorsForSSC(
                    Stream.of(spIdA).collect(Collectors.toSet()),
                    preflightRequestCollectors,
                    pushVulnerabilitiesCollectors);

            //
            //  I
            //  add one client and verify it works okay
            //
            OctaneConfiguration configA = new OctaneConfigurationIntern(clientAInstanceId, OctaneSPEndpointSimulator.getSimulatorUrl(), spIdA);
            OctaneClient clientA = OctaneSDK.addClient(configA, VulnerabilitiesServicePluginServicesTest.class);
            VulnerabilitiesService vulnerabilitiesServiceA = clientA.getVulnerabilitiesService();

            prepareSSCSimulator();
            vulnerabilitiesServiceA.enqueueRetrieveAndPushVulnerabilities("jobSSC1", "1",ToolType.SSC, System.currentTimeMillis(), 1,null, null);


            GeneralTestUtils.waitAtMostFor(12000, () -> {
                if (preflightRequestCollectors.get(spIdA) != null && preflightRequestCollectors.get(spIdA).size() == 1) {
                    return true;
                } else {
                    return null;
                }
            });

            Assert.assertEquals(clientAInstanceId + "|jobSSC1|1", preflightRequestCollectors.get(spIdA).get(0));

//			try {
//				Thread.sleep(1000*60*10);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}

            //
            //  III
            //  remove one client and verify it is shut indeed and the second continue to work okay
            //
            OctaneSDK.removeClient(clientA);

        } finally {
            //  remove clients
            OctaneSDK.getClients().forEach(OctaneSDK::removeClient);

            //  remove simulators
            if (simulators != null) removeSPEPSimulators(simulators.values());
            SSCServerSimulator.instance().endSimulation();
        }
    }

    @Test(timeout = 20000)
    public void testABUpdatedCClosedDMissingENewNoBaseline() throws IOException {

        Issues.Issue issueA = new Issues.Issue();
        issueA.issueInstanceId = "A";
        issueA.scanStatus  = "UPDATED";
        issueA.id = 1;

        Issues.Issue issueB = new Issues.Issue();
        issueB.issueInstanceId = "B";
        issueB.scanStatus  = "UPDATED";
        issueB.id = 2;

        Issues.Issue issueD = new Issues.Issue();
        issueD.issueInstanceId = "D";
        issueD.scanStatus  = "UPDATED";
        issueD.id = 4;

        Issues.Issue issueE = new Issues.Issue();
        issueE.issueInstanceId = "E";
        issueE.scanStatus = "NEW";
        issueE.id = 5;

        SSCInput sscInput = SSCInput.buildInputWithDefaults(
                Arrays.asList(issueA, issueB, issueD, issueE));
        OctaneInput octaneInput = new OctaneInput();
        octaneInput.remoteIds = Arrays.asList("A", "B", "C");
        octaneInput.baseline = null;


        ExpectedPushToOctane expectedPushToOctane = new ExpectedPushToOctane();
        expectedPushToOctane.newIssues = Arrays.asList(issueE);
        expectedPushToOctane.updateIssues = Arrays.asList(issueA, issueB);
        expectedPushToOctane.closedIssuesStillExistingInOctane = Arrays.asList("C");
        expectedPushToOctane.missingHasExtendedData = false;
        expectedPushToOctane.missingIssues = Arrays.asList(issueD);


        SSCIntegrationTest sscIntegrationTest = new SSCIntegrationTest(octaneInput,
                sscInput,
                expectedPushToOctane);

        String errorMsg = sscIntegrationTest.runAndGetErrorMsg();
        Assert.assertNull(errorMsg);
    }

    @Test(timeout = 20000)
    public void testABUpdatedCClosedDMissingENewFGBeforeBaseline() throws IOException {


        DateFormat sourceDateFormat = new SimpleDateFormat(DateUtils.sscFormat);
        String nowString = sourceDateFormat.format(new Date());
        String yesterdayString = sourceDateFormat.format(new Date(System.currentTimeMillis() - 1000*60*60*24));
        String threeDaysAgoString = sourceDateFormat.format(new Date(System.currentTimeMillis() - 1000*60*60*72));

        Issues.Issue issueA = new Issues.Issue();
        issueA.issueInstanceId = "A";
        issueA.scanStatus  = "UPDATED";
        issueA.id = 1;
        issueA.foundDate = yesterdayString;

        Issues.Issue issueB = new Issues.Issue();
        issueB.issueInstanceId = "B";
        issueB.scanStatus  = "UPDATED";
        issueB.id = 2;
        issueB.foundDate = yesterdayString;

        Issues.Issue issueE = new Issues.Issue();
        issueE.issueInstanceId = "E";
        issueE.scanStatus = "NEW";
        issueE.id = 5;
        issueE.foundDate =nowString;

        Issues.Issue issueD = new Issues.Issue();
        issueD.issueInstanceId = "D";
        issueD.scanStatus  = "UPDATED";
        issueD.id = 4;
        issueD.foundDate = yesterdayString;

        Issues.Issue issueF = new Issues.Issue();
        issueF.issueInstanceId = "F";
        issueF.scanStatus  = "UPDATED";
        issueF.id = 6;
        issueF.foundDate = threeDaysAgoString;

        Issues.Issue issueG = new Issues.Issue();
        issueG.issueInstanceId = "G";
        issueG.scanStatus  = "UPDATED";
        issueG.id = 7;
        issueG.foundDate = threeDaysAgoString;

        SSCInput sscInput = SSCInput.buildInputWithDefaults(Arrays.asList(
                issueA,issueB,issueD, issueE, issueF, issueG));

        OctaneInput octaneInput = new OctaneInput();
        //Baseline = two days ago.
        octaneInput.baseline = new Date(System.currentTimeMillis() - 1000*60*60*48);
        octaneInput.remoteIds = Arrays.asList("A","B","C");

        ExpectedPushToOctane expectedPushToOctane = new ExpectedPushToOctane();
        expectedPushToOctane.updateIssues = Arrays.asList(issueA, issueB);
        expectedPushToOctane.closedIssuesStillExistingInOctane = Arrays.asList("C");
        expectedPushToOctane.newIssues = Arrays.asList(issueE);
        expectedPushToOctane.missingIssues = Arrays.asList(issueD);
        expectedPushToOctane.beforeBaselineIssues = Arrays.asList(issueF, issueG);
        expectedPushToOctane.missingHasExtendedData = true;

        SSCIntegrationTest sscIntegrationTest = new SSCIntegrationTest(octaneInput, sscInput,
                expectedPushToOctane);

        String errorMsg = sscIntegrationTest.runAndGetErrorMsg();
        Assert.assertNull(errorMsg);
    }

    @Test(timeout = 20000)
    public void testABDMissingENewFGBeforeBaseline() throws IOException {


        DateFormat sourceDateFormat = new SimpleDateFormat(DateUtils.sscFormat);
        String nowString = sourceDateFormat.format(new Date());
        String yesterdayString = sourceDateFormat.format(new Date(System.currentTimeMillis() - 1000*60*60*24));
        String threeDaysAgoString = sourceDateFormat.format(new Date(System.currentTimeMillis() - 1000*60*60*72));


        Issues.Issue issueE = new Issues.Issue();
        issueE.issueInstanceId = "E";
        issueE.scanStatus = "NEW";
        issueE.id = 5;
        issueE.foundDate =nowString;


        Issues.Issue issueA = new Issues.Issue();
        issueA.issueInstanceId = "A";
        issueA.scanStatus  = "UPDATED";
        issueA.id = 1;
        issueA.foundDate = yesterdayString;

        Issues.Issue issueB = new Issues.Issue();
        issueB.issueInstanceId = "B";
        issueB.scanStatus  = "UPDATED";
        issueB.id = 2;
        issueB.foundDate = yesterdayString;

        Issues.Issue issueD = new Issues.Issue();
        issueD.issueInstanceId = "D";
        issueD.scanStatus  = "UPDATED";
        issueD.id = 4;
        issueD.foundDate = yesterdayString;

        Issues.Issue issueF = new Issues.Issue();
        issueF.issueInstanceId = "F";
        issueF.scanStatus  = "UPDATED";
        issueF.id = 6;
        issueF.foundDate = threeDaysAgoString;

        Issues.Issue issueG = new Issues.Issue();
        issueG.issueInstanceId = "G";
        issueG.scanStatus  = "UPDATED";
        issueG.id = 7;
        issueG.foundDate = threeDaysAgoString;

        SSCInput sscInput = SSCInput.buildInputWithDefaults(Arrays.asList(
                issueA, issueB, issueD, issueE
        ));
        OctaneInput octaneInput = new OctaneInput();
        octaneInput.remoteIds = new ArrayList<>();
        octaneInput.baseline = new Date(System.currentTimeMillis() - 1000*60*60*48);

        ExpectedPushToOctane expectedPushToOctane = new ExpectedPushToOctane();
        expectedPushToOctane.missingIssues = Arrays.asList(issueA, issueB, issueD);
        expectedPushToOctane.missingHasExtendedData = true;
        expectedPushToOctane.beforeBaselineIssues = Arrays.asList(issueF, issueG);
        expectedPushToOctane.newIssues = Arrays.asList(issueE);

        SSCIntegrationTest sscIntegrationTest = new SSCIntegrationTest(octaneInput,
                sscInput, expectedPushToOctane);
        String errorMsg = sscIntegrationTest.runAndGetErrorMsg();
        Assert.assertNull(errorMsg);
    }

    @Test(timeout = 20000)
    public void testABCClosedENEW() throws IOException {


        DateFormat sourceDateFormat = new SimpleDateFormat(DateUtils.sscFormat);
        String nowString = sourceDateFormat.format(new Date());


        Issues.Issue issueE = new Issues.Issue();
        issueE.issueInstanceId = "E";
        issueE.scanStatus = "NEW";
        issueE.id = 5;
        issueE.foundDate =nowString;

        SSCInput sscInput = SSCInput.buildInputWithDefaults(Arrays.asList(issueE));

        OctaneInput octaneInput = new OctaneInput();
        octaneInput.baseline = new Date(System.currentTimeMillis() - 1000*60*60*48);
        octaneInput.remoteIds = Arrays.asList("A","B","C");

        ExpectedPushToOctane expectedPushToOctane = new ExpectedPushToOctane();
        expectedPushToOctane.closedIssuesStillExistingInOctane = Arrays.asList("A","B","C");
        expectedPushToOctane.newIssues = Arrays.asList(issueE);

        SSCIntegrationTest sscIntegrationTest = new SSCIntegrationTest(octaneInput,
                sscInput, expectedPushToOctane);
        String errorMsg = sscIntegrationTest.runAndGetErrorMsg();
        Assert.assertNull(errorMsg);
    }

    @Test(timeout = 20000)
    public void testNoPushToOctane() throws IOException {


        SSCInput sscInput = SSCInput.buildInputWithDefaults(new ArrayList<>());

        OctaneInput octaneInput = new OctaneInput();
        octaneInput.baseline = new Date(System.currentTimeMillis() - 1000*60*60*48);
        octaneInput.remoteIds = new ArrayList<>();

        ExpectedPushToOctane expectedPushToOctane = new ExpectedPushToOctane();
        expectedPushToOctane.expectNoPsuh = true;

        SSCIntegrationTest sscIntegrationTest = new SSCIntegrationTest(octaneInput,
                sscInput, expectedPushToOctane);
        String errorMsg = sscIntegrationTest.runAndGetErrorMsg();
        Assert.assertNull(errorMsg);
    }

    private Map<String, OctaneSPEndpointSimulator> initSPEPSimulators(
            Set<String> spIDs,
            Map<String, List<String>> preflightRequestsCollectors,
            Map<String, List<String>> pushRequestCollectors) {
        Map<String, OctaneSPEndpointSimulator> result = new LinkedHashMap<>();

        for (String spID : spIDs) {
            OctaneSPEndpointSimulator simulator = OctaneSPEndpointSimulator.addInstance(spID);

            //  vulnerabilities preflight API
            simulator.installApiHandler(HttpMethod.GET, "^.*/vulnerabilities/preflight$", request -> {
                try {
                    //  retrieve query parameters
                    request.mergeQueryParameters("", request.getQueryString(), false);
                    preflightRequestsCollectors
                            .computeIfAbsent(spID, sid -> new LinkedList<>())
                            .add(request.getQueryParameters().getString("instance-id") + "|" +
                                    request.getQueryParameters().getString("job-ci-id") + "|" +
                                    request.getQueryParameters().getString("build-ci-id"));
                    request.getResponse().setStatus(HttpStatus.SC_OK);
                    request.getResponse().getWriter().write(request.getQueryParameters().getString("job-ci-id").contains("true") ? "true" : "false");
                    request.getResponse().getWriter().flush();
                } catch (IOException ioe) {
                    throw new OctaneSDKGeneralException("failed to write response", ioe);
                }
            });

            //  vulnerabilities push API
            simulator.installApiHandler(HttpMethod.POST, "^.*/vulnerabilities$", request -> {
                try {
                    String rawVulnerabilitiesBody = CIPluginSDKUtils.inputStreamToUTF8String(new GZIPInputStream(request.getInputStream()));
                    pushRequestCollectors
                            .computeIfAbsent(spID, sid -> new LinkedList<>())
                            .add(rawVulnerabilitiesBody);
                    request.getResponse().setStatus(HttpStatus.SC_ACCEPTED);
                    request.getResponse().getWriter().write("{\"status\": \"queued\"}");
                    request.getResponse().getWriter().flush();
                } catch (IOException ioe) {
                    throw new OctaneSDKGeneralException("failed to write response", ioe);
                }
            });

            result.put(spID, simulator);
        }

        return result;
    }

    private Map<String, OctaneSPEndpointSimulator> initSPEPSimulatorsForSSC(
            Set<String> spIDs,
            Map<String, List<String>> preflightRequestsCollectors,
            Map<String, List<String>> pushRequestCollectors) {
        Map<String, OctaneSPEndpointSimulator> result = new LinkedHashMap<>();

        for (String spID : spIDs) {
            OctaneSPEndpointSimulator simulator = OctaneSPEndpointSimulator.addInstance(spID);

            //  vulnerabilities preflight API
            simulator.installApiHandler(HttpMethod.GET, "^.*/vulnerabilities/preflight$", request -> {
                try {
                    //  retrieve query parameters
                    request.mergeQueryParameters("", request.getQueryString(), false);
                    preflightRequestsCollectors
                            .computeIfAbsent(spID, sid -> new LinkedList<>())
                            .add(request.getQueryParameters().getString("instance-id") + "|" +
                                    request.getQueryParameters().getString("job-ci-id") + "|" +
                                    request.getQueryParameters().getString("build-ci-id"));
                    request.getResponse().setStatus(HttpStatus.SC_OK);
                    request.getResponse().getWriter().write("true");
                    request.getResponse().getWriter().flush();
                } catch (IOException ioe) {
                    throw new OctaneSDKGeneralException("failed to write response", ioe);
                }
            });

            //  vulnerabilities push API
            simulator.installApiHandler(HttpMethod.POST, "^.*/vulnerabilities$", request -> {
                try {
                    String rawVulnerabilitiesBody = CIPluginSDKUtils.inputStreamToUTF8String(new GZIPInputStream(request.getInputStream()));
                    pushRequestCollectors
                            .computeIfAbsent(spID, sid -> new LinkedList<>())
                            .add(rawVulnerabilitiesBody);
                    request.getResponse().setStatus(HttpStatus.SC_ACCEPTED);
                    request.getResponse().getWriter().write("{\"status\": \"queued\"}");
                    request.getResponse().getWriter().flush();
                } catch (IOException ioe) {
                    throw new OctaneSDKGeneralException("failed to write response", ioe);
                }
            });

            result.put(spID, simulator);
        }

        return result;
    }

    private void prepareSSCSimulator() {
        try {
            SSCServerSimulator.instance().startServer();
            SSCServerSimulator.instance().setDefaultAuth();

            SSCInput sscInput = new SSCInput();
            sscInput.withProject("project-a",1)
                    .withProjectVersion("version-a", 100);

            sscInput.setArtifactsPage(
                    SSCInput.createArtifacts(System.currentTimeMillis() + 5000,"ERROR_PROCESSING"));

            SSCServerSimulator.instance().setSequenceToSimulator(sscInput);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void removeSPEPSimulators(Collection<OctaneSPEndpointSimulator> simulators) {
        for (OctaneSPEndpointSimulator simulator : simulators) {
            try {
                OctaneSPEndpointSimulator.removeInstance(simulator.getSharedSpaceId());
            } catch (Exception e) {
                logger.error("failed to remove Octane SharedSpace Simulator", e);
            }
        }
    }
}

