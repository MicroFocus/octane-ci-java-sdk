/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this path except in compliance with the License.
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
    private String path;
    private Integer sumOfCoveredLines;
    private Integer totalCoverableLines;


    public String getPath() {
        return path;
    }

    public FileCoverage setPath(String path) {
        this.path = path;
        return this;
    }

    public Integer getSumOfCoveredLines() {
        return sumOfCoveredLines;
    }

    public FileCoverage setSumOfCoveredLines(Integer sumOfCoveredLines) {
        this.sumOfCoveredLines = sumOfCoveredLines;
        return this;
    }

    public Integer getTotalCoverableLines() {
        return totalCoverableLines;
    }

    public FileCoverage setTotalCoverableLines(Integer totalCodeLinesInFile) {
        this.totalCoverableLines = totalCodeLinesInFile;
        return this;
    }
}
