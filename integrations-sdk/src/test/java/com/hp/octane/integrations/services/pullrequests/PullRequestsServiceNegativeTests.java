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

package com.hp.octane.integrations.services.pullrequests;

import com.hp.octane.integrations.OctaneSDK;
import org.junit.Test;

public class PullRequestsServiceNegativeTests {

	@Test(expected = IllegalArgumentException.class)
	public void testA() {
		new PullRequestServiceImpl(null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testB() {
		new PullRequestServiceImpl((OctaneSDK.SDKServicesConfigurer) new Object(), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testC() {
		PullRequestService.newInstance(null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testD() {
		PullRequestService.newInstance((OctaneSDK.SDKServicesConfigurer) new Object(), null);
	}
}
