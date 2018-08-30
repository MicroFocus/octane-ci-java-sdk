package com.hp.octane.integrations.utils;

import com.hp.octane.integrations.util.CIPluginSDKUtils;
import org.junit.Assert;
import org.junit.Test;

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

	private Object objectFromForeignThread() {
		Object[] resultHolder = new Object[1];
		new Thread(() -> resultHolder[0] = new Object()).start();
		CIPluginSDKUtils.doWait(200);
		return resultHolder[0];
	}
}
