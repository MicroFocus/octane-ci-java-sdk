/*
 * Copyright 2017-2026 Open Text
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Octane SDK tests
 */

public class OctaneSDKNegativeTests {
    private static DTOFactory dtoFactory = DTOFactory.getInstance();

    @Test
    public void sdkTestNegativeA() {
        assertThrows(IllegalArgumentException.class, () ->
            OctaneSDK.addClient(null, null));
    }

    //  bad plugin services class
    @Test
    public void sdkTestNegativeC() {
        assertThrows(IllegalArgumentException.class, () -> {
            OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", UUID.randomUUID().toString(), null, null);
            OctaneSDK.addClient(oc, null);
        });
    }

    @Test
    public void sdkTestNegativeC1() {
        assertThrows(IllegalArgumentException.class, () -> {
            OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", UUID.randomUUID().toString(), null, null);
            OctaneSDK.addClient(oc, PluginServices1.class);
        });
    }

    @Test
    public void sdkTestNegativeC2() {
        assertThrows(IllegalArgumentException.class, () -> {
            OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", UUID.randomUUID().toString(), null, null);
            OctaneSDK.addClient(oc, PluginServices2.class);
        });
    }

    @Test
    public void sdkTestNegativeC3() {
        assertThrows(IllegalArgumentException.class, () -> {
            OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", UUID.randomUUID().toString(), null, null);
            OctaneSDK.addClient(oc, PluginServices3.class);
        });
    }

    //  duplicate OctaneConfiguration instance
    @Test
    public void sdkTestNegativeE1() {
        assertThrows(IllegalStateException.class, () -> {
            OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", UUID.randomUUID().toString(), null, null);
            OctaneClient successfulOne = OctaneSDK.addClient(oc, PluginServices.class);
            try {
                OctaneSDK.addClient(oc, PluginServices.class);
            } finally {
                Assertions.assertNotNull(OctaneSDK.removeClient(successfulOne));
            }
        });
    }

    //  duplicate instance ID
    @Test
    public void sdkTestNegativeE2() {
        assertThrows(IllegalStateException.class, () -> {
            String sp1 = UUID.randomUUID().toString();
            String sp2 = UUID.randomUUID().toString();
            OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", sp1, null, null);
            OctaneConfiguration oc2 = new OctaneConfigurationIntern(oc1.getInstanceId(), "http://localhost", sp2, null, null);
            OctaneClient successfulOne = OctaneSDK.addClient(oc1, PluginServices.class);
            try {
                OctaneSDK.addClient(oc2, PluginServices.class);
            } finally {
                Assertions.assertNotNull(OctaneSDK.removeClient(successfulOne));
            }
        });
    }

    //  duplicate shared space ID
    @Test
    public void sdkTestNegativeE3() {
        assertThrows(IllegalStateException.class, () -> {
            String sp = UUID.randomUUID().toString();
            OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", sp, null, null);
            OctaneConfiguration oc2 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", sp, null, null);
            OctaneClient successfulOne = OctaneSDK.addClient(oc1, PluginServices.class);
            try {
                OctaneSDK.addClient(oc2, PluginServices.class);
            } finally {
                Assertions.assertNotNull(OctaneSDK.removeClient(successfulOne));
            }
        });
    }

    //  instance ID on plugin service
    @Test
    public void sdkTestNegativeF1() {
        assertThrows(IllegalArgumentException.class, () -> {
            CIPluginServices ps = new PluginServices();
            Assertions.assertNull(ps.getInstanceId());
            ps.setInstanceId(null);
        });
    }

    @Test
    public void sdkTestNegativeF2() {
        assertThrows(IllegalArgumentException.class, () -> {
            CIPluginServices ps = new PluginServices();
            Assertions.assertNull(ps.getInstanceId());
            ps.setInstanceId("");
        });
    }

    @Test
    public void sdkTestNegativeF3() {
        assertThrows(IllegalStateException.class, () -> {
            String instanceId = UUID.randomUUID().toString();
            String sp = UUID.randomUUID().toString();
            OctaneConfiguration oc = new OctaneConfigurationIntern(instanceId, "http://localhost", sp, null, null);
            OctaneClient client = OctaneSDK.addClient(oc, PluginServices4.class);
            try {
                //  verify existing
                Assertions.assertNotNull(PluginServices4.proxyGetInstanceId());
                Assertions.assertEquals(instanceId, PluginServices4.proxyGetInstanceId());

                //  set to the same does nothing and not throws
                PluginServices4.proxySetInstanceId(instanceId);
                Assertions.assertEquals(instanceId, PluginServices4.proxyGetInstanceId());

                //  set to something else throws IllegalStateException
                PluginServices4.proxySetInstanceId(UUID.randomUUID().toString());
            } finally {
                Assertions.assertNotNull(OctaneSDK.removeClient(client));
            }
        });
    }

    //  get client by instance ID
    @Test
    public void sdkTestNegativeG() {
        assertThrows(IllegalArgumentException.class, () ->
            OctaneSDK.getClientByInstanceId(null));
    }

    @Test
    public void sdkTestNegativeH() {
        assertThrows(IllegalArgumentException.class, () ->
            OctaneSDK.getClientByInstanceId(""));
    }

    @Test
    public void sdkTestNegativeI() {
        assertThrows(IllegalStateException.class, () ->
            OctaneSDK.getClientByInstanceId("none-existing-one"));
    }

    //  remove client
    @Test
    public void sdkTestNegativeL() {
        assertThrows(IllegalArgumentException.class, () ->
            OctaneSDK.removeClient(null));
    }

    @Test
    public void sdkTestNegativeM() {
        String sp1 = UUID.randomUUID().toString();
        String sp2 = UUID.randomUUID().toString();
        OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", sp1, null, null);
        OctaneConfiguration oc2 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", sp2, null, null);
        OctaneClient successfulOne = OctaneSDK.addClient(oc1, PluginServices.class);
        OctaneClient successfulTwo = OctaneSDK.addClient(oc2, PluginServices.class);
        OctaneClient removed = OctaneSDK.removeClient(successfulOne);
        Assertions.assertNull(OctaneSDK.removeClient(removed));
        Assertions.assertNotNull(OctaneSDK.removeClient(successfulTwo));
    }

    @Test
    public void sdkTestNegativeN() {
        String sp = UUID.randomUUID().toString();
        OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", sp, null, null);
        OctaneClient successfulOne = OctaneSDK.addClient(oc, PluginServices.class);
        Assertions.assertNotNull(OctaneSDK.removeClient(successfulOne));
        Assertions.assertNull(OctaneSDK.removeClient(successfulOne));
    }

    //  client dynamically breaks unique instanceId/farm/sharedSpaceId contract
    @Test
    public void sdkTestNegativeO1() {
        assertThrows(IllegalArgumentException.class, () -> {
            String sp1 = UUID.randomUUID().toString();
            String sp2 = UUID.randomUUID().toString();
            OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", sp1, null, null);
            OctaneConfiguration oc2 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", sp2, null, null);
            OctaneClient clientA = OctaneSDK.addClient(oc1, PluginServices.class);
            OctaneClient clientB = OctaneSDK.addClient(oc2, PluginServices.class);
            Assertions.assertNotNull(clientA);
            Assertions.assertNotNull(clientB);

            try {
                oc1.setUrlAndSpace(oc2.getUrl(), oc2.getSharedSpace());
            } finally {
                Assertions.assertNotNull(OctaneSDK.removeClient(clientA));
                Assertions.assertNotNull(OctaneSDK.removeClient(clientB));
            }
        });
    }

    @Test
    public void sdkTestNegativeO2() {
        assertThrows(IllegalArgumentException.class, () -> {
            String url1 = "http://localhost";
            String url2 = "http://localhost1";
            String sp = UUID.randomUUID().toString();
            OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), url1, sp, null, null);
            OctaneConfiguration oc2 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), url2, sp, null, null);
            OctaneClient clientA = OctaneSDK.addClient(oc1, PluginServices.class);
            OctaneClient clientB = OctaneSDK.addClient(oc2, PluginServices.class);
            Assertions.assertNotNull(clientA);
            Assertions.assertNotNull(clientB);

            try {
                oc1.setUrlAndSpace(oc2.getUrl(), oc2.getSharedSpace());
            } finally {
                Assertions.assertNotNull(OctaneSDK.removeClient(clientA));
                Assertions.assertNotNull(OctaneSDK.removeClient(clientB));
            }
        });
    }

    @Test
    public void sdkTestNegativeO3() {
        assertThrows(IllegalArgumentException.class, () -> {
            String url1 = "http://localhost:8080";
            String url2 = "http://localhost:8081";
            String sp = UUID.randomUUID().toString();
            OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), url1, sp, null, null);
            OctaneConfiguration oc2 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), url2, sp, null, null);
            OctaneClient clientA = OctaneSDK.addClient(oc1, PluginServices.class);
            OctaneClient clientB = OctaneSDK.addClient(oc2, PluginServices.class);
            Assertions.assertNotNull(clientA);
            Assertions.assertNotNull(clientB);

            try {
                oc1.setUrlAndSpace(oc2.getUrl(), oc2.getSharedSpace());
            } finally {
                Assertions.assertNotNull(OctaneSDK.removeClient(clientA));
                Assertions.assertNotNull(OctaneSDK.removeClient(clientB));
            }
        });
    }

    @Test
    public void sdkTestNegativeO4() {
        assertThrows(IllegalArgumentException.class, () -> {
            String url1 = "http://localhost:8080";
            String url2 = "http://localhost:8081";
            String sp = UUID.randomUUID().toString();
            OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), url1, sp, null, null);
            OctaneConfiguration oc2 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), url2, sp, null, null);
            OctaneClient clientA = OctaneSDK.addClient(oc1, PluginServices.class);
            OctaneClient clientB = OctaneSDK.addClient(oc2, PluginServices.class);
            Assertions.assertNotNull(clientA);
            Assertions.assertNotNull(clientB);

            try {
                oc1.setUrlAndSpace(oc2.getUrl(), oc2.getSharedSpace());
            } finally {
                Assertions.assertNotNull(OctaneSDK.removeClient(clientA));
                Assertions.assertNotNull(OctaneSDK.removeClient(clientB));
            }
        });
    }

    @Test
    public void sdkTestNegativeO5() {
        assertThrows(IllegalArgumentException.class, () -> {
            String sp = UUID.randomUUID().toString();
            OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", sp, null, null);
            OctaneClient client = OctaneSDK.addClient(oc, PluginServices.class);
            Assertions.assertNotNull(client);

            try {
                oc.setUrlAndSpace(oc.getUrl(), null);
            } finally {
                Assertions.assertNotNull(OctaneSDK.removeClient(client));
            }
        });
    }

    @Test
    public void sdkTestNegativeO6() {
        assertThrows(IllegalArgumentException.class, () -> {
            String sp = UUID.randomUUID().toString();
            OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", sp, null, null);
            OctaneClient client = OctaneSDK.addClient(oc, PluginServices.class);
            Assertions.assertNotNull(client);

            try {
                oc.setUrlAndSpace(oc.getUrl(), "");
            } finally {
                Assertions.assertNotNull(OctaneSDK.removeClient(client));
            }
        });
    }

    //  illegal OctaneConfiguration properties for test Octane configuration
    @Test
    public void sdkTestNegativeQ1() throws IOException {
        assertThrows(IllegalArgumentException.class, () ->
            OctaneSDK.testOctaneConfigurationAndFetchAvailableWorkspaces(null, null, null, null, null));
    }

    @Test
    public void sdkTestNegativeQ2() throws IOException {
        assertThrows(IllegalArgumentException.class, () ->
            OctaneSDK.testOctaneConfigurationAndFetchAvailableWorkspaces("non-valid-url", null, null, null, null));
    }

    @Test
    public void sdkTestNegativeQ3() throws IOException {
        assertThrows(IllegalArgumentException.class, () ->
            OctaneSDK.testOctaneConfigurationAndFetchAvailableWorkspaces("http://localhost:9999", null, null, null, null));
    }

    @Test
    public void sdkTestNegativeQ4() throws IOException {
        assertThrows(IllegalArgumentException.class, () ->
            OctaneSDK.testOctaneConfigurationAndFetchAvailableWorkspaces("http://localhost:9999", "", null, null, null));
    }

    @Test
    public void sdkTestNegativeQ5() throws IOException {
        assertThrows(IllegalArgumentException.class, () ->
            OctaneSDK.testOctaneConfigurationAndFetchAvailableWorkspaces("http://localhost:9999", "1001", null, null, null));
    }

    @Test
    public void sdkTestNegativeQ6() throws IOException {
        assertThrows(IllegalArgumentException.class, () ->
            OctaneSDK.testOctaneConfigurationAndFetchAvailableWorkspaces("http://localhost:9999", "1001", null, null, PluginServices3.class));
    }

    //  illegal OctaneClient creation
    @Test
    public void sdkTestNegativeR() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OctaneClientImpl(null);
        });
    }

    @Test
    public void sdkTestNegativeS() {
        try {
            OctaneSDK.SDKServicesConfigurer.class.getConstructor(OctaneConfiguration.class, CIPluginServices.class).newInstance(null, null);
            Assertions.fail("should not be able to create");
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Assertions.assertNotNull(e);
        }
    }

    //  MOCK classes
    public static class PluginServices extends CIPluginServices {
        @Override
        public CIServerInfo getServerInfo() {
            return dtoFactory.newDTO(CIServerInfo.class);
        }

        @Override
        public CIPluginInfo getPluginInfo() {
            return dtoFactory.newDTO(CIPluginInfo.class);
        }
    }

    public static class PluginServices1 extends CIPluginServices {
        @Override
        public CIServerInfo getServerInfo() {
            return null;
        }

        @Override
        public CIPluginInfo getPluginInfo() {
            return dtoFactory.newDTO(CIPluginInfo.class);
        }
    }

    public static class PluginServices2 extends CIPluginServices {
        @Override
        public CIServerInfo getServerInfo() {
            return dtoFactory.newDTO(CIServerInfo.class);
        }

        @Override
        public CIPluginInfo getPluginInfo() {
            return null;
        }
    }

    private static class PluginServices3 extends CIPluginServices {
        @Override
        public CIServerInfo getServerInfo() {
            return dtoFactory.newDTO(CIServerInfo.class);
        }

        @Override
        public CIPluginInfo getPluginInfo() {
            return dtoFactory.newDTO(CIPluginInfo.class);
        }
    }

    public static class PluginServices4 extends CIPluginServices {
        private static CIPluginServices instance;

        public PluginServices4() {
            instance = this;
        }

        @Override
        public CIServerInfo getServerInfo() {
            return dtoFactory.newDTO(CIServerInfo.class);
        }

        @Override
        public CIPluginInfo getPluginInfo() {
            return dtoFactory.newDTO(CIPluginInfo.class);
        }

        private static String proxyGetInstanceId() {
            return instance.getInstanceId();
        }

        private static void proxySetInstanceId(String instanceId) {
            instance.setInstanceId(instanceId);
        }
    }
}
