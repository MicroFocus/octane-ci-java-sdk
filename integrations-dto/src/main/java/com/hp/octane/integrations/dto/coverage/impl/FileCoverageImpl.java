/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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

package com.hp.octane.integrations.dto.coverage.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.coverage.FileCoverage;
import com.hp.octane.integrations.dto.coverage.LineCoverage;

/**
 * FileCoverage DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class FileCoverageImpl implements FileCoverage {
	private String file;
	private LineCoverage[] lines;

	public String getFile() {
		return file;
	}

	public FileCoverage setFile(String file) {
		this.file = file;
		return this;
	}

	public LineCoverage[] getLines() {
		return lines;
	}

	public FileCoverage setLines(LineCoverage[] lines) {
		this.lines = lines;
		return this;
	}
}
