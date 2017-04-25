package com.hp.octane.integrations.dto.executor.impl;

import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.scm.SCMRepository;

/**
 * Created by shitritn on 4/3/2017.
 */
public class TestConnectivityInfoImpl implements TestConnectivityInfo {
	private SCMRepository scmRepository;
	private String usernmae;
	private String password;

	@Override
	public SCMRepository getScmRepository() {
		return scmRepository;
	}

	@Override
	public TestConnectivityInfo setScmRepository(SCMRepository scmRepository) {
		this.scmRepository = scmRepository;
		return this;
	}

	@Override
	public String getUsername() {
		return this.usernmae;
	}

	@Override
	public TestConnectivityInfo setUsername(String username) {
		this.usernmae = username;
		return this;
	}

	@Override
	public String getPassword() {
		return this.password;
	}

	@Override
	public TestConnectivityInfo setPassword(String password) {
		this.password = password;
		return this;
	}
}
