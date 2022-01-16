package com.hp.octane.integrations;

import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class OctaneConfigurationTests {

	//  illegal instance ID
	@Test(expected = IllegalArgumentException.class)
	public void testA1() {
		new OctaneConfigurationIntern(null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testA2() {
		new OctaneConfigurationIntern("", null, null);
	}

	//  illegal URL
	@Test(expected = IllegalArgumentException.class)
	public void testB1() {
		new OctaneConfigurationIntern(UUID.randomUUID().toString(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testB2() {
		new OctaneConfigurationIntern(UUID.randomUUID().toString(), "", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testB3() {
		new OctaneConfigurationIntern(UUID.randomUUID().toString(), "non-valid-url", null);
	}

	//  illegal shared space ID
	@Test(expected = IllegalArgumentException.class)
	public void testC1() {
		new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:9999", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testC2() {
		new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:9999", "");
	}

	@Test
	public void testD() {
		OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:9999/some/path?query=false", "1002");
		Assert.assertNotNull(oc);
		Assert.assertEquals("http://localhost:9999/some/path", oc.getUrl());
		Assert.assertNotNull(oc.toString());
		Assert.assertFalse(oc.toString().isEmpty());
		Assert.assertFalse(oc.attached);
	}

	@Test
	public void testE() {
		OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:9999/some/path?query=false", "1002");
		Assert.assertNotNull(oc);

		oc.setUiLocation("https://some.host.com/some/path/ui?query=false&p=1002");
		Assert.assertEquals("https://some.host.com/some/path", oc.getUrl());

		oc.setUiLocation("http://localhost.end/ui?&p=1002");
		Assert.assertEquals("http://localhost.end", oc.getUrl());

		oc.setUiLocation("http://localhost.end:9999/ui?&p=1003");
		Assert.assertEquals("http://localhost.end:9999", oc.getUrl());
		Assert.assertEquals("1003", oc.getSharedSpace());
	}
}
