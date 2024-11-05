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

import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneConfigurationIntern;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.exceptions.OctaneSDKGeneralException;
import com.hp.octane.integrations.testhelpers.GeneralTestUtils;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;
import com.hp.octane.integrations.testhelpers.SSCServerSimulator;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.Assert;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class SSCIntegrationTest {

    private static final Logger logger = LogManager.getLogger(SSCIntegrationTest.class);


    private String spIdA = UUID.randomUUID().toString();
    private String clientAInstanceId = UUID.randomUUID().toString();
    private Map<String, List<String>> preFlightRequestCollectors = new LinkedHashMap<>();
    private Map<String, List<String>> pushVulnerabilitiesCollectors = new LinkedHashMap<>();
    private Map<String, OctaneSPEndpointSimulator> simulators = null;

    private ExpectedPushToOctane expectedOutput;
    private SSCInput sscInput;
    private OctaneInput octaneInput;

    public SSCIntegrationTest(OctaneInput octaneInput,
                              SSCInput sscInput,
                              ExpectedPushToOctane expectedPushToOctane) {

        this.octaneInput = octaneInput;
        this.sscInput = sscInput;
        this.expectedOutput = expectedPushToOctane;
    }


    public void perpareOctaneSimulator(){

        simulators = initSPEPSimulatorsForSSC(
                Stream.of(spIdA).collect(Collectors.toSet()),
                preFlightRequestCollectors,
                pushVulnerabilitiesCollectors);

    }
    public void runAsQueueItem(){

        try {
            //
            //  I
            //  add one client and verify it works okay
            //
            OctaneConfiguration configA = new OctaneConfigurationIntern(clientAInstanceId, OctaneSPEndpointSimulator.getSimulatorUrl(), spIdA);
            OctaneClient clientA = OctaneSDK.addClient(configA, VulnerabilitiesServicePluginServicesTest.class);
            VulnerabilitiesService vulnerabilitiesServiceA = clientA.getVulnerabilitiesService();


            vulnerabilitiesServiceA.enqueueRetrieveAndPushVulnerabilities("jobSSC1",
                    "1",ToolType.SSC, System.currentTimeMillis(), 1,null, null);

            if(expectedOutput.expectNoPsuh) {
                CIPluginSDKUtils.doWait(2000);
            }else{
                GeneralTestUtils.waitAtMostFor(12000, () -> {
                    if (pushVulnerabilitiesCollectors.get(spIdA) != null && pushVulnerabilitiesCollectors.get(spIdA).size() == 1) {
                        return true;
                    } else {
                        return null;
                    }
                });
            }

            Assert.assertEquals(clientAInstanceId + "|" + CIPluginSDKUtils.urlEncodeBase64("jobSSC1") + "|1", preFlightRequestCollectors.get(spIdA).get(0));

            //
            //  III
            //  remove one client and verify it is shut indeed and the second continue to work okay
            //
            OctaneSDK.removeClient(clientA);
        }finally {
            //  remove clients
            OctaneSDK.getClients().forEach(OctaneSDK::removeClient);

            //  remove simulators
            if (simulators != null) removeSPEPSimulators(simulators.values());
        }


    }
    public String runAndGetErrorMsg() throws IOException {
        try {
            SSCServerSimulator.instance().setDefaultAuth();
            SSCServerSimulator.instance().setSequenceToSimulator(getSscInput());
            SSCServerSimulator.instance().startServer();

            perpareOctaneSimulator();
            runAsQueueItem();

            validateRunResult();
            return null;
        } catch (IssuesValidate.SSCTestFailure e) {
            return e.getSSCFailureMessage();
        } finally {
            SSCServerSimulator.instance().endSimulation();
        }
    }

    private void validateRunResult() throws IOException, IssuesValidate.SSCTestFailure {
        if(expectedOutput.expectNoPsuh){
            if(this.pushVulnerabilitiesCollectors.get(spIdA) != null){
                throw new RuntimeException("Unexpected push to octane was performed");
            }
        }else {
            String octaneIssues = this.pushVulnerabilitiesCollectors.get(spIdA).get(0);
            IssuesValidate validate = new IssuesValidate();
            validate.validateOutput(octaneIssues, this.expectedOutput);
        }
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
                    request.mergeQueryParameters("", request.getQueryString());
                    preflightRequestsCollectors
                            .computeIfAbsent(spID, sid -> new LinkedList<>())
                            .add(request.getQueryParameters().getString("instance-id") + "|" +
                                    request.getQueryParameters().getString("job-ci-id") + "|" +
                                    request.getQueryParameters().getString("build-ci-id"));
                    request.getResponse().setStatus(HttpStatus.SC_OK);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateUtils.octaneFormat);

                    request.getResponse().getWriter().write(getOctaneInput().baseline == null ? "true" :
                             simpleDateFormat.format(getOctaneInput().baseline));
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

            //  vulnerabilities push API
            simulator.installApiHandler(HttpMethod.GET, "^.*/vulnerabilities/remote-issue-ids.*", request -> {
                try {

                    request.getResponse().setStatus(HttpStatus.SC_OK);

                    request.getResponse().getWriter().write(SSCTestUtils.getJson(this.getOctaneInput().remoteIds));
                    request.getResponse().getWriter().flush();
                } catch (IOException ioe) {
                    throw new OctaneSDKGeneralException("failed to write response", ioe);
                }
            });

            result.put(spID, simulator);
        }

        return result;
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

    public SSCInput getSscInput() {
        return sscInput;
    }



    public OctaneInput getOctaneInput() {
        return octaneInput;
    }

}
