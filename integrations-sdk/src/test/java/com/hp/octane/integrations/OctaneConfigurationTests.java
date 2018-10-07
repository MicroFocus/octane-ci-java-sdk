package com.hp.octane.integrations;

import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class OctaneConfigurationTests {

	//  illegal instance ID
	@Test(expected = IllegalArgumentException.class)
	public void testA1() {
		new OctaneConfiguration(null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testA2() {
		new OctaneConfiguration("", null, null);
	}

	//  illegal URL
	@Test(expected = IllegalArgumentException.class)
	public void testB1() {
		new OctaneConfiguration(UUID.randomUUID().toString(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testB2() {
		new OctaneConfiguration(UUID.randomUUID().toString(), "", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testB3() {
		new OctaneConfiguration(UUID.randomUUID().toString(), "non-valid-url", null);
	}

	//  illegal shared space ID
	@Test(expected = IllegalArgumentException.class)
	public void testC1() {
		new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:9999", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testC2() {
		new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:9999", "");
	}

	@Test
	public void testD() {
		OctaneConfiguration oc = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:9999/some/path?query=false", "1002");
		Assert.assertNotNull(oc);
		Assert.assertEquals("http://localhost:9999", oc.getUrl());
		Assert.assertNotNull(oc.toString());
		Assert.assertFalse(oc.toString().isEmpty());
		Assert.assertFalse(oc.attached);
	}

	@Test
	public void testE() {
		OctaneConfiguration oc = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:9999/some/path?query=false", "1002");
		Assert.assertNotNull(oc);

		oc.setUrl("https://some.host.com/some/path?query=false");
		Assert.assertEquals("https://some.host.com", oc.getUrl());

		oc.setUrl("http://localhost.end");
		Assert.assertEquals("http://localhost.end", oc.getUrl());

		oc.setUrl("http://localhost.end:9999");
		Assert.assertEquals("http://localhost.end:9999", oc.getUrl());

		oc.setSharedSpace("1002");
		Assert.assertEquals("1002", oc.getSharedSpace());

		oc.setSharedSpace("1001");
		Assert.assertEquals("1001", oc.getSharedSpace());
	}
}
