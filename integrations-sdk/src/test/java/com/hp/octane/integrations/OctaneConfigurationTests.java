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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class OctaneConfigurationTests {

	//  illegal instance ID
	@Test
	public void testA1() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OctaneConfigurationIntern(null, null, null);
        });
    }

	@Test
	public void testA2() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OctaneConfigurationIntern("", null, null);
        });
    }

	//  illegal URL
	@Test
	public void testB1() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OctaneConfigurationIntern(UUID.randomUUID().toString(), null, null);
        });
    }

	@Test
	public void testB2() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OctaneConfigurationIntern(UUID.randomUUID().toString(), "", null);
        });
    }

	@Test
	public void testB3() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OctaneConfigurationIntern(UUID.randomUUID().toString(), "non-valid-url", null);
        });
    }

	//  illegal shared space ID
	@Test
	public void testC1() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:9999", null);
        });
    }

	@Test
	public void testC2() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:9999", "");
        });
    }

	@Test
	public void testD() {
		OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:9999/some/path?query=false", "1002");
		Assertions.assertNotNull(oc);
		Assertions.assertEquals("http://localhost:9999/some/path", oc.getUrl());
		Assertions.assertNotNull(oc.toString());
		Assertions.assertFalse(oc.toString().isEmpty());
		Assertions.assertFalse(oc.attached);
	}

	@Test
	public void testE() {
		OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:9999/some/path?query=false", "1002");
		Assertions.assertNotNull(oc);

		oc.setUiLocation("https://some.host.com/some/path/ui?query=false&p=1002");
		Assertions.assertEquals("https://some.host.com/some/path", oc.getUrl());

		oc.setUiLocation("http://localhost.end/ui?&p=1002");
		Assertions.assertEquals("http://localhost.end", oc.getUrl());

		oc.setUiLocation("http://localhost.end:9999/ui?&p=1003");
		Assertions.assertEquals("http://localhost.end:9999", oc.getUrl());
		Assertions.assertEquals("1003", oc.getSharedSpace());
	}
}
