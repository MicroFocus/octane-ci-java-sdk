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
package com.hp.octane.integrations.utils;

import com.hp.octane.integrations.exceptions.OctaneSDKGeneralException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CIPluginSDKUtilsTest {

	@Test
	public void testDoWaitBadParameter() {
        assertThrows(IllegalArgumentException.class, () ->
            CIPluginSDKUtils.doWait(0));
    }

	@Test
	public void testDoWaitNoInterrupt() {
		long started = System.currentTimeMillis();
		long timeToWait = 2000;

		CIPluginSDKUtils.doWait(timeToWait);
		long ended = System.currentTimeMillis();
		Assertions.assertTrue(ended - started >= timeToWait);
	}

	@Test
	public void testDoWaitWithInterrupt() {
		long started = System.currentTimeMillis();
		long timeToWait = 2000;
		Thread sleepingThread = Thread.currentThread();

		new Thread(() -> {
			CIPluginSDKUtils.doWait(timeToWait / 2);
			sleepingThread.interrupt();
		}).start();
		CIPluginSDKUtils.doWait(timeToWait);
		long ended = System.currentTimeMillis();
		Assertions.assertTrue(ended - started >= timeToWait);
	}

	@Test
	public void testDoBreakableWaitBadParameterA() {
        assertThrows(IllegalArgumentException.class, () ->
            CIPluginSDKUtils.doBreakableWait(0, null));
    }

	@Test
	public void testDoBreakableWaitBadParameterB() {
        assertThrows(IllegalArgumentException.class, () ->
            CIPluginSDKUtils.doBreakableWait(1, null));
    }

	@Test
	public void testDoBreakableWaitNoInterrupt() {
		long started = System.currentTimeMillis();
		long timeToWait = 2000;
		Object monitor = objectFromForeignThread();

		CIPluginSDKUtils.doBreakableWait(timeToWait, monitor);
		long ended = System.currentTimeMillis();
		Assertions.assertTrue(ended - started >= timeToWait);
	}

	@Test
	public void testDoBreakableWaitWithInterruptErroneous() {
		long started = System.currentTimeMillis();
		long timeToWait = 2000;
		Object monitor = objectFromForeignThread();
		Thread sleepingThread = Thread.currentThread();

		new Thread(() -> {
			CIPluginSDKUtils.doWait(timeToWait / 2);
			synchronized (monitor) {
				sleepingThread.interrupt();
			}
		}).start();
		CIPluginSDKUtils.doBreakableWait(timeToWait, monitor);
		long ended = System.currentTimeMillis();
		Assertions.assertTrue(ended - started >= timeToWait);
	}

	@Test
	public void testDoBreakableWaitWithInterruptIntentional() {
		long started = System.currentTimeMillis();
		long timeToWait = 2000;
		Object monitor = objectFromForeignThread();

		new Thread(() -> {
			CIPluginSDKUtils.doWait(timeToWait / 2);
			synchronized (monitor) {
				monitor.notify();
			}
		}).start();
		CIPluginSDKUtils.doBreakableWait(timeToWait, monitor);
		long ended = System.currentTimeMillis();
		Assertions.assertTrue(ended - started > timeToWait / 2);
		Assertions.assertTrue(ended - started < timeToWait);
	}

	@Test
	public void testInputStreamToStringA() throws IOException {
        assertThrows(IllegalArgumentException.class, () ->
            CIPluginSDKUtils.inputStreamToUTF8String(null));
    }

	@Test
	public void testInputStreamToStringB() throws IOException {
        assertThrows(IllegalArgumentException.class, () ->
            CIPluginSDKUtils.inputStreamToString(null, null));
    }

	@Test
	public void testInputStreamToStringC() throws IOException {
        assertThrows(IllegalArgumentException.class, () ->
            CIPluginSDKUtils.inputStreamToString(new ByteArrayInputStream("some text".getBytes()), null));
    }

	@Test
	public void testInputStreamToStringD() throws IOException {
		String text = "some text to test";

		String test = CIPluginSDKUtils.inputStreamToString(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8.name())), StandardCharsets.UTF_8);
		Assertions.assertEquals(text, test);

		test = CIPluginSDKUtils.inputStreamToString(new ByteArrayInputStream(text.getBytes()), StandardCharsets.UTF_8);
		Assertions.assertEquals(text, test);
	}

	@Test
	public void testInputStreamToStringE() throws IOException {
		String text = "some text to test וגם בעברית и по русски чуток";

		String test = CIPluginSDKUtils.inputStreamToString(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8.name())), StandardCharsets.UTF_8);
		Assertions.assertEquals(text, test);

		//  the case below may fail on unpredictable default charset in different environments, temporary disabled
//		test = CIPluginSDKUtils.inputStreamToString(new ByteArrayInputStream(text.getBytes()), Charset.defaultCharset());
//		Assert.assertEquals(text, test);
	}

	@Test
	public void testInputStreamToStringF() throws IOException {
		String text = "some text to test וגם בעברית и по русски чуток";
		String test = CIPluginSDKUtils.inputStreamToUTF8String(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8.name())));
		Assertions.assertEquals(text, test);
	}

	@Test
	public void testsParseURLPos() {
		URL url = CIPluginSDKUtils.parseURL("http://localhost:8080");
		Assertions.assertNotNull(url);
	}

	@Test
	public void testsParseURLNeg1() {
        assertThrows(IllegalArgumentException.class, () ->
            CIPluginSDKUtils.parseURL("something-wrong-here"));
    }

	@Test
	public void testsParseURLNeg2() {
        assertThrows(IllegalArgumentException.class, () ->
            CIPluginSDKUtils.parseURL(null));
    }

	@Test
	public void testsParseURLNeg3() {
        assertThrows(IllegalArgumentException.class, () ->
            CIPluginSDKUtils.parseURL(""));
    }

	@Test
	public void testURLEncodePathParamsPos() {
		String encoded = CIPluginSDKUtils.urlEncodePathParam("some string to . be in path");
		Assertions.assertEquals("some%20string%20to%20.%20be%20in%20path", encoded);
	}

	@Test
	public void testURLEncodePathParamsNeg1() {
		String encoded = CIPluginSDKUtils.urlEncodePathParam(null);
		Assertions.assertNull(encoded);
	}

	@Test
	public void testURLEncodePathParamsPos2() {
		String encoded = CIPluginSDKUtils.urlEncodePathParam("");
		Assertions.assertEquals("", encoded);
	}

	@Test
	public void testURLEncodeQueryParamsPos() {
		String encoded = CIPluginSDKUtils.urlEncodeQueryParam("some string to . be in path");
		Assertions.assertEquals("some+string+to+.+be+in+path", encoded);
	}

	@Test
	public void testURLEncodeQueryParamsNeg1() {
		String encoded = CIPluginSDKUtils.urlEncodeQueryParam(null);
		Assertions.assertNull(encoded);
	}

	@Test
	public void testURLEncodeQueryParamsPos2() {
		String encoded = CIPluginSDKUtils.urlEncodeQueryParam("");
		Assertions.assertEquals("", encoded);
	}

	//  is non-proxy host tests
	@Test
	public void testIsNotProxyHostNeg() {
		boolean result = CIPluginSDKUtils.isNonProxyHost(null, null);
		Assertions.assertFalse(result);

		result = CIPluginSDKUtils.isNonProxyHost("", null);
		Assertions.assertFalse(result);

		result = CIPluginSDKUtils.isNonProxyHost("some", null);
		Assertions.assertFalse(result);

		result = CIPluginSDKUtils.isNonProxyHost("some", "");
		Assertions.assertFalse(result);
	}

	@Test
	public void testIsNotProxyHost() {
		boolean result = CIPluginSDKUtils.isNonProxyHost("some", "some");
		Assertions.assertTrue(result);

		result = CIPluginSDKUtils.isNonProxyHost("some.host", "some");
		Assertions.assertTrue(result);

		result = CIPluginSDKUtils.isNonProxyHost("some", "some.host");
		Assertions.assertFalse(result);
	}

	@Test
	public void testIsNotProxyHostWildcard() {
		boolean result = CIPluginSDKUtils.isNonProxyHost("some", "some*");
		Assertions.assertTrue(result);

		result = CIPluginSDKUtils.isNonProxyHost("some.host", "*me.ho*");
		Assertions.assertTrue(result);

		result = CIPluginSDKUtils.isNonProxyHost("some", "s*e");
		Assertions.assertTrue(result);
	}

	@Test
	public void testIsNotProxyHostWildcardMulti() {
		boolean result = CIPluginSDKUtils.isNonProxyHost("some", "localhost|some*");
		Assertions.assertTrue(result);

		result = CIPluginSDKUtils.isNonProxyHost("some.host", "*me.ho*|localhost");
		Assertions.assertTrue(result);

		result = CIPluginSDKUtils.isNonProxyHost("some", "first|s*e|last");
		Assertions.assertTrue(result);

		result = CIPluginSDKUtils.isNonProxyHost("some", "first|s*e||||last");
		Assertions.assertTrue(result);

		result = CIPluginSDKUtils.isNonProxyHost("some", "first  |s*e|||    |la,st");
		Assertions.assertTrue(result);
	}

	@Test
	public void testIsNotProxyHostWildcardMultiWithQuotations() {
		boolean result = CIPluginSDKUtils.isNonProxyHost("some", "'localhost|some*'");
		Assertions.assertTrue(result);

		result = CIPluginSDKUtils.isNonProxyHost("some.host", "\"*me.ho*|localhost\"");
		Assertions.assertTrue(result);

		result = CIPluginSDKUtils.isNonProxyHost("some", "\"first|s*e|last\"");
		Assertions.assertTrue(result);

		result = CIPluginSDKUtils.isNonProxyHost("some", "\"first|s*e||||last\"");
		Assertions.assertTrue(result);

		result = CIPluginSDKUtils.isNonProxyHost("some", "'first  |s*e|||    |la,st'");
		Assertions.assertTrue(result);
	}

	private Object objectFromForeignThread() {
		Object[] resultHolder = new Object[1];
		new Thread(() -> resultHolder[0] = new Object()).start();
		CIPluginSDKUtils.doWait(200);
		return resultHolder[0];
	}
}
