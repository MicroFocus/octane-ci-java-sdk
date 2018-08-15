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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.hp.octane.integrations.dto.coverage.BuildCoverage;
import com.hp.octane.integrations.dto.coverage.FileCoverage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * BuildCoverage DTO implementation.
 */


@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildCoverageImpl implements BuildCoverage {


    private Integer numberOfFiles;
    private List<FileCoverage> fileCoverageList;
    private Integer sumOfCoveredLines;
    private Integer totalCoverableLines;
    private String projectName;

    static ObjectMapper objectMapper = new ObjectMapper();

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

    public Integer getNumberOfFiles() {
        return numberOfFiles;
    }

    public BuildCoverage setNumberOfFiles(Integer numberOfFiles) {
        this.numberOfFiles = numberOfFiles;
        return this;
    }

    public List<FileCoverage> getFileCoverageList() {
        return fileCoverageList;
    }

    public BuildCoverage setFileCoverageList(List<FileCoverage> fileCoverageList) {
        this.fileCoverageList = fileCoverageList;
        return this;
    }

    public BuildCoverage mergeSonarCoverageReport(InputStream report) {
        JsonNode jsonReport;
        try {
            jsonReport = objectMapper.readTree(report);
            if (this.projectName == null) {
                this.numberOfFiles = jsonReport.get("paging").get("total").asInt();

                JsonNode baseComponentJson = jsonReport.get("baseComponent");
                this.projectName = baseComponentJson.get("name").textValue();
                //add measures
                JsonNode measuresArray = baseComponentJson.get("measures");
                this.totalCoverableLines = getIntegerValueFromMeasuresArray("lines_to_cover", measuresArray);
                Integer uncoveredLines = getIntegerValueFromMeasuresArray("uncovered_lines", measuresArray);
                this.sumOfCoveredLines = this.totalCoverableLines - uncoveredLines;

            }
            ArrayNode componentsArray = (ArrayNode) jsonReport.get("fileCoverageList");
            for (JsonNode component : componentsArray) {
                FileCoverage fileCoverage = getFileCoverageFromJson(component);
                this.fileCoverageList.add(fileCoverage);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
                .setFile(fileComponent.get("path").textValue())
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