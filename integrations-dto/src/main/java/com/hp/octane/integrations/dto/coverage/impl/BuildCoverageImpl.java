/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
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

package com.hp.octane.integrations.dto.coverage.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.hp.octane.integrations.dto.coverage.BuildCoverage;
import com.hp.octane.integrations.dto.coverage.FileCoverage;

import java.util.ArrayList;
import java.util.List;

/**
 * BuildCoverage DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildCoverageImpl implements BuildCoverage {
	private List<FileCoverage> fileCoverageList = new ArrayList<>();
	private Integer sumOfCoveredLines;
	private Integer totalCoverableLines;
	private String projectName;

	public List<FileCoverage> getFileCoverageList() {
		return fileCoverageList;
	}

	public BuildCoverage setFileCoverageList(List<FileCoverage> fileCoverageList) {
		this.fileCoverageList = fileCoverageList;
		return this;
	}

	public Integer getSumOfCoveredLines() {
		return sumOfCoveredLines;
	}

	public BuildCoverage setSumOfCoveredLines(Integer sumOfCoveredLines) {
		this.sumOfCoveredLines = sumOfCoveredLines;
		return this;
	}

	public Integer getTotalCoverableLines() {
		return totalCoverableLines;
	}

	public BuildCoverage setTotalCoverableLines(Integer totalCoverableLines) {
		this.totalCoverableLines = totalCoverableLines;
		return this;
	}

	public String getProjectName() {
		return projectName;
	}

	public BuildCoverage setProjectName(String projectName) {
		this.projectName = projectName;
		return this;
	}

	public BuildCoverage mergeSonarCoverageReport(JsonNode jsonReport) {
		if (projectName == null) {
			JsonNode baseComponentJson = jsonReport.get("baseComponent");
			projectName = baseComponentJson.get("name").textValue();
			//add measures
			JsonNode measuresArray = baseComponentJson.get("measures");
			totalCoverableLines = getIntegerValueFromMeasuresArray("lines_to_cover", measuresArray);
			Integer uncoveredLines = getIntegerValueFromMeasuresArray("uncovered_lines", measuresArray);
			sumOfCoveredLines = totalCoverableLines - uncoveredLines;
		}

		ArrayNode componentsArray = (ArrayNode) jsonReport.get("components");
		for (JsonNode component : componentsArray) {
			FileCoverage fileCoverage = getFileCoverageFromJson(component);
			fileCoverageList.add(fileCoverage);
		}

		return this;
	}

	private FileCoverage getFileCoverageFromJson(JsonNode fileComponent) {
		Integer uncoveredLines;
		Integer linesToCover;

		JsonNode measuresArray = fileComponent.get("measures");
		linesToCover = getIntegerValueFromMeasuresArray("lines_to_cover", measuresArray);
		uncoveredLines = getIntegerValueFromMeasuresArray("uncovered_lines", measuresArray);

		return new FileCoverageImpl()
				.setPath(fileComponent.get("path").textValue())
				.setSumOfCoveredLines(linesToCover - uncoveredLines)
				.setTotalCoverableLines(linesToCover);
	}

	private Integer getIntegerValueFromMeasuresArray(String metric, JsonNode measures) {
		for (JsonNode measure : measures) {
			if (measure.get("metric").textValue().equals(metric)) {
				return Integer.parseInt(measure.get("value").textValue());
			}
		}
		return 0;
	}
}