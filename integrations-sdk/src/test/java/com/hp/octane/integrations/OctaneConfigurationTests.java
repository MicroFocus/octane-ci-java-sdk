package com.hp.octane.integrations;

import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class OctaneConfigurationTests {

	//  illegal instance ID
	@Test(expected = IllegalArgumentException.class)
	public void testA1() {
		OctaneConfiguration.createWithUiLocation(null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testA2() {
		OctaneConfiguration.createWithUiLocation("", null);
	}

	//  illegal URL
	@Test(expected = IllegalArgumentException.class)
	public void testB1() {
		OctaneConfiguration.createWithUiLocation(UUID.randomUUID().toString(), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testB2() {
		OctaneConfiguration.createWithUiLocation(UUID.randomUUID().toString(), "");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testB3() {
		OctaneConfiguration.createWithUiLocation(UUID.randomUUID().toString(), "non-valid-url");
	}

	//  illegal shared space ID
	@Test(expected = IllegalArgumentException.class)
	public void testC1() {
		OctaneConfiguration.createWithUiLocation(UUID.randomUUID().toString(), "http://localhost:9999");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testC2() {
		OctaneConfiguration.createWithUiLocation(UUID.randomUUID().toString(), "http://localhost:9999");
	}

	@Test
	public void testD() {
		OctaneConfiguration oc = OctaneConfiguration.createWithUiLocation(UUID.randomUUID().toString(), "http://localhost:9999/some/path/ui?p=1002&query=false");
		Assert.assertNotNull(oc);
		Assert.assertEquals("http://localhost:9999/some/path", oc.getUrl());
		Assert.assertNotNull(oc.toString());
		Assert.assertFalse(oc.toString().isEmpty());
		Assert.assertFalse(oc.attached);
	}

	@Test
	public void testE() {
		OctaneConfiguration oc = OctaneConfiguration.createWithUiLocation(UUID.randomUUID().toString(), "http://localhost:9999/some/path/ui?p=1002&query=false");
		Assert.assertNotNull(oc);

		oc.setUiLocation("http://localhost/bubu/ui?p=123&query=false");
		Assert.assertEquals("http://localhost/bubu", oc.getUrl());
		Assert.assertEquals("123", oc.getSharedSpace());
	}
}
