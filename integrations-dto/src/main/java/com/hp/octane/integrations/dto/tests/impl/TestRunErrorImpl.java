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
 *
 */

package com.hp.octane.integrations.dto.tests.impl;

import com.hp.octane.integrations.dto.tests.TestRunError;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * TestRunError DTO implementation.
 */

@XmlRootElement(name = "error")
@XmlAccessorType(XmlAccessType.NONE)
public class TestRunErrorImpl implements TestRunError {

	@XmlAttribute(name = "type")
	private String errorType;

	@XmlAttribute(name = "message")
	private String errorMessage;

	@XmlValue
	private String stackTrace;

	public String getErrorType() {
		return errorType;
	}

	public TestRunError setErrorType(String errorType) {
		this.errorType = errorType;
		return this;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public TestRunError setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
		return this;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public TestRunError setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
		return this;
	}
}
