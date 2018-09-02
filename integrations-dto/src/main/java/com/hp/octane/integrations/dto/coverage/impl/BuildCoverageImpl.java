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

    private List<FileCoverage> fileCoverageList;
    private Integer sumOfCoveredLines;
    private Integer totalCoverableLines;
    private String projectName;

    public BuildCoverageImpl() {
        fileCoverageList = new ArrayList<>();
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


    public List<FileCoverage> getFileCoverageList() {
        return fileCoverageList;
    }

    public BuildCoverage setFileCoverageList(List<FileCoverage> fileCoverageList) {
        this.fileCoverageList = fileCoverageList;
        return this;
    }

    public BuildCoverage mergeSonarCoverageReport(JsonNode jsonReport) {
        if (this.projectName == null) {

            JsonNode baseComponentJson = jsonReport.get("baseComponent");
            this.projectName = baseComponentJson.get("name").textValue();
            //add measures
            JsonNode measuresArray = baseComponentJson.get("measures");
            this.totalCoverableLines = getIntegerValueFromMeasuresArray("lines_to_cover", measuresArray);
            Integer uncoveredLines = getIntegerValueFromMeasuresArray("uncovered_lines", measuresArray);
            this.sumOfCoveredLines = this.totalCoverableLines - uncoveredLines;

        }
        ArrayNode componentsArray = (ArrayNode) jsonReport.get("components");
        for (JsonNode component : componentsArray) {
            FileCoverage fileCoverage = getFileCoverageFromJson(component);
            this.fileCoverageList.add(fileCoverage);
        }

        return this;

    }

    private FileCoverage getFileCoverageFromJson(JsonNode fileComponent) {
        Integer uncoveredLines = 0;
        Integer linesToCover = 0;

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