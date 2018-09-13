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

package com.hp.octane.integrations.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CIPluginSDKUtilsTest {

	@Test(expected = IllegalArgumentException.class)
	public void testDoWaitBadParameter() {
		CIPluginSDKUtils.doWait(0);
	}

	@Test
	public void testDoWaitNoInterrupt() {
		long started = System.currentTimeMillis();
		long timeToWait = 2000;

		CIPluginSDKUtils.doWait(timeToWait);
		long ended = System.currentTimeMillis();
		Assert.assertTrue(ended - started >= timeToWait);
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
		Assert.assertTrue(ended - started >= timeToWait);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDoBreakableWaitBadParameterA() {
		CIPluginSDKUtils.doBreakableWait(0, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDoBreakableWaitBadParameterB() {
		CIPluginSDKUtils.doBreakableWait(1, null);
	}

	@Test
	public void testDoBreakableWaitNoInterrupt() {
		long started = System.currentTimeMillis();
		long timeToWait = 2000;
		Object monitor = objectFromForeignThread();

		CIPluginSDKUtils.doBreakableWait(timeToWait, monitor);
		long ended = System.currentTimeMillis();
		Assert.assertTrue(ended - started >= timeToWait);
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
		Assert.assertTrue(ended - started >= timeToWait);
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
		Assert.assertTrue(ended - started > timeToWait / 2);
		Assert.assertTrue(ended - started < timeToWait);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInputStreamToStringA() throws IOException {
		CIPluginSDKUtils.inputStreamToUTF8String(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInputStreamToStringB() throws IOException {
		CIPluginSDKUtils.inputStreamToString(null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInputStreamToStringC() throws IOException {
		CIPluginSDKUtils.inputStreamToString(new ByteArrayInputStream("some text".getBytes()), null);
	}

	@Test
	public void testInputStreamToStringD() throws IOException {
		String text = "some text to test";

		String test = CIPluginSDKUtils.inputStreamToString(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8.name())), StandardCharsets.UTF_8);
		Assert.assertEquals(text, test);

		test = CIPluginSDKUtils.inputStreamToString(new ByteArrayInputStream(text.getBytes()), StandardCharsets.UTF_8);
		Assert.assertEquals(text, test);
	}

	@Test
	public void testInputStreamToStringE() throws IOException {
		String text = "some text to test וגם בעברית и по русски чуток";

		String test = CIPluginSDKUtils.inputStreamToString(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8.name())), StandardCharsets.UTF_8);
		Assert.assertEquals(text, test);

		//  the case below may fail on unpredictable default charset in different environments, temporary disabled
//		test = CIPluginSDKUtils.inputStreamToString(new ByteArrayInputStream(text.getBytes()), Charset.defaultCharset());
//		Assert.assertEquals(text, test);
	}


	@Test
	public void testInputStreamToStringF() throws IOException {
		String text = "some text to test וגם בעברית и по русски чуток";

		String test = CIPluginSDKUtils.inputStreamToUTF8String(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8.name())));
		Assert.assertEquals(text, test);
	}

	private Object objectFromForeignThread() {
		Object[] resultHolder = new Object[1];
		new Thread(() -> resultHolder[0] = new Object()).start();
		CIPluginSDKUtils.doWait(200);
		return resultHolder[0];
	}
}
