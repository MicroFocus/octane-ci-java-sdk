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
package com.hp.octane.integrations;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Octane SDK tests
 */

public class OctaneSDKPositiveTests {
    private static DTOFactory dtoFactory = DTOFactory.getInstance();

    @Test
    public void sdkTestA() {
        List<OctaneClient> octaneClients = OctaneSDK.getClients();
        Assert.assertNotNull(octaneClients);
        String instance1 = UUID.randomUUID().toString();
        String instance2 = UUID.randomUUID().toString();
        OctaneConfiguration oc1 = new OctaneConfigurationIntern(instance1, "http://localhost", "1001", null, null);
        OctaneConfiguration oc2 = new OctaneConfigurationIntern(instance2, "http://localhost", "1002", null, null);

        OctaneSDK.addClient(oc1, PluginServices.class);
        OctaneSDK.addClient(oc2, PluginServices.class);

        octaneClients = OctaneSDK.getClients();
        Assert.assertNotNull(octaneClients);
        Assert.assertFalse(octaneClients.isEmpty());

        OctaneClient client = OctaneSDK.getClientByInstanceId(oc1.getInstanceId());
        Assert.assertNotNull(client);
        Assert.assertEquals(instance1, client.getInstanceId());
        Assert.assertEquals(oc1, client.getConfigurationService().getConfiguration());

        client = OctaneSDK.getClientByInstanceId(oc2.getInstanceId());
        Assert.assertNotNull(client);
        Assert.assertEquals(instance2, client.getInstanceId());
        Assert.assertEquals(oc2, client.getConfigurationService().getConfiguration());

        OctaneSDK.getClients().forEach(OctaneSDK::removeClient);
    }

    @Test
    public void sdkTestB() {
        String instance = UUID.randomUUID().toString();
        String url = "http://localhost:8080";
        String sp = UUID.randomUUID().toString();
        OctaneConfiguration oc = new OctaneConfigurationIntern(instance, url, sp, null, null);

        OctaneSDK.addClient(oc, PluginServices.class);

        OctaneClient client = OctaneSDK.getClientByInstanceId(oc.getInstanceId());
        Assert.assertNotNull(client);
        Assert.assertEquals(instance, client.getInstanceId());
        Assert.assertEquals(url, client.getConfigurationService().getConfiguration().getUrl());
        Assert.assertEquals(sp, client.getConfigurationService().getConfiguration().getSharedSpace());

        //  same values should work smooth
        oc.setUrlAndSpace(url, sp);
        Assert.assertEquals(url, client.getConfigurationService().getConfiguration().getUrl());
        Assert.assertEquals(sp, client.getConfigurationService().getConfiguration().getSharedSpace());

        //  new unique values should work as well
        url = "http://localhost:8081";
        sp = UUID.randomUUID().toString();
        oc.setUrlAndSpace(url, sp);
        Assert.assertEquals(url, client.getConfigurationService().getConfiguration().getUrl());
        Assert.assertEquals(sp, client.getConfigurationService().getConfiguration().getSharedSpace());

        OctaneSDK.getClients().forEach(OctaneSDK::removeClient);
    }

    @Test
    public void sdkTestD() {
        OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
        OctaneConfiguration oc2 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1002", null, null);
        OctaneClient clientA = OctaneSDK.addClient(oc1, PluginServices.class);
        OctaneClient clientB = OctaneSDK.addClient(oc2, PluginServices.class);

        try {
            Assert.assertNotNull(clientA);
            Assert.assertNotNull(clientB);

            Assert.assertNotNull(clientA.getConfigurationService());
            Assert.assertNotNull(clientA.getCoverageService());
            Assert.assertNotNull(clientA.getSonarService());
            Assert.assertNotNull(clientA.getEntitiesService());
            Assert.assertNotNull(clientA.getEventsService());
            Assert.assertNotNull(clientA.getLogsService());
            Assert.assertNotNull(clientA.getPipelineContextService());
            Assert.assertNotNull(clientA.getRestService());
            Assert.assertNotNull(clientA.getTasksProcessor());
            Assert.assertNotNull(clientA.getTestsService());
            Assert.assertNotNull(clientA.getRestService());
            Assert.assertNotNull(clientA.getVulnerabilitiesService());

            Assert.assertNotNull(clientB.getConfigurationService());
            Assert.assertNotNull(clientB.getCoverageService());
            Assert.assertNotNull(clientB.getSonarService());
            Assert.assertNotNull(clientB.getEntitiesService());
            Assert.assertNotNull(clientB.getEventsService());
            Assert.assertNotNull(clientB.getLogsService());
            Assert.assertNotNull(clientB.getPipelineContextService());
            Assert.assertNotNull(clientB.getRestService());
            Assert.assertNotNull(clientB.getTasksProcessor());
            Assert.assertNotNull(clientB.getTestsService());
            Assert.assertNotNull(clientB.getRestService());
            Assert.assertNotNull(clientB.getVulnerabilitiesService());

            Assert.assertNotEquals(clientA.getConfigurationService(), clientB.getConfigurationService());
            Assert.assertNotEquals(clientA.getCoverageService(), clientB.getCoverageService());
            Assert.assertNotEquals(clientA.getSonarService(), clientB.getSonarService());
            Assert.assertNotEquals(clientA.getEntitiesService(), clientB.getEntitiesService());
            Assert.assertNotEquals(clientA.getEventsService(), clientB.getEventsService());
            Assert.assertNotEquals(clientA.getLogsService(), clientB.getLogsService());
            Assert.assertNotEquals(clientA.getPipelineContextService(), clientB.getPipelineContextService());
            Assert.assertNotEquals(clientA.getRestService(), clientB.getRestService());
            Assert.assertNotEquals(clientA.getTasksProcessor(), clientB.getTasksProcessor());
            Assert.assertNotEquals(clientA.getTestsService(), clientB.getTestsService());
            Assert.assertNotEquals(clientA.getRestService(), clientB.getRestService());
            Assert.assertNotEquals(clientA.getVulnerabilitiesService(), clientB.getVulnerabilitiesService());
        } finally {
            Assert.assertNotNull(OctaneSDK.removeClient(clientA));
            Assert.assertNotNull(OctaneSDK.removeClient(clientB));
        }
    }

    @Test
    public void sdkTestE() {
        OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
        OctaneClient client = OctaneSDK.addClient(oc, PluginServices.class);

        Assert.assertEquals("OctaneClientImpl{ instanceId: " + oc.getInstanceId() + " }", client.toString());

        Assert.assertNotNull(OctaneSDK.removeClient(client));
    }

    @Test
    public void sdkTestF() {
        OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
        OctaneConfiguration oc2 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1002", null, null);
        OctaneClient clientA = OctaneSDK.addClient(oc1, OctaneSDKNegativeTests.PluginServices.class);
        OctaneClient clientB = OctaneSDK.addClient(oc2, OctaneSDKNegativeTests.PluginServices.class);
        Assert.assertNotNull(clientA);
        Assert.assertNotNull(clientB);

        Assert.assertNotNull(OctaneSDK.removeClient(clientB));
        oc2.setUrlAndSpace(oc2.getUrl(), oc1.getSharedSpace());
    }

    public static class PluginServices extends CIPluginServices {
        @Override
        public CIServerInfo getServerInfo() {
            return dtoFactory.newDTO(CIServerInfo.class);
        }

        @Override
        public CIPluginInfo getPluginInfo() {
            return dtoFactory.newDTO(CIPluginInfo.class);
        }

        @Override
        public File getAllowedOctaneStorage() {
            return new File("temp");
        }
    }
}
