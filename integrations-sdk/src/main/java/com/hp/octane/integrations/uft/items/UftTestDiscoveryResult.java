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

package com.hp.octane.integrations.uft.items;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import org.apache.commons.codec.Charsets;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This file represents result of UFT discovery (tests and data tables)
 */
public class UftTestDiscoveryResult implements Serializable {

    private List<AutomatedTest> tests = new ArrayList<>();

    private List<ScmResourceFile> scmResourceFiles = new ArrayList<>();

    private List<String> deletedFolders = new ArrayList<>();

    private String scmRepositoryId;

    private String testRunnerId;

    private String workspaceId;

    private String configurationId;

    private TestingToolType testingToolType;

    private boolean fullScan;

    private boolean hasQuotedPaths;

    @JsonIgnore
    private List<AutomatedTest> getTestByOctaneStatus(OctaneStatus status) {
        List<AutomatedTest> filtered = new ArrayList<>();
        for (AutomatedTest test : tests) {
            if (test.getOctaneStatus().equals(status)) {
                filtered.add(test);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    @JsonIgnore
    private List<ScmResourceFile> getResourceFilesByOctaneStatus(OctaneStatus status) {
        List<ScmResourceFile> filtered = new ArrayList<>();
        for (ScmResourceFile file : scmResourceFiles) {
            if (file.getOctaneStatus().equals(status)) {
                filtered.add(file);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    @JsonIgnore
    public List<AutomatedTest> getNewTests() {
        return getTestByOctaneStatus(OctaneStatus.NEW);
    }

    @JsonIgnore
    public List<AutomatedTest> getDeletedTests() {
        return getTestByOctaneStatus(OctaneStatus.DELETED);
    }

    @JsonIgnore
    public List<AutomatedTest> getUpdatedTests() {
        return getTestByOctaneStatus(OctaneStatus.MODIFIED);
    }

    public String getScmRepositoryId() {
        return scmRepositoryId;
    }

    public void setScmRepositoryId(String scmRepositoryId) {
        this.scmRepositoryId = scmRepositoryId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public boolean isFullScan() {
        return fullScan;
    }

    public void setFullScan(boolean fullScan) {
        this.fullScan = fullScan;
    }

    @JsonIgnore
    public boolean hasChanges() {
        return !getAllScmResourceFiles().isEmpty() || !getAllTests().isEmpty() || !getDeletedFolders().isEmpty();
    }

    @JsonIgnore
    public List<ScmResourceFile> getNewScmResourceFiles() {
        return getResourceFilesByOctaneStatus(OctaneStatus.NEW);
    }

    @JsonIgnore
    public List<ScmResourceFile> getDeletedScmResourceFiles() {
        return getResourceFilesByOctaneStatus(OctaneStatus.DELETED);
    }

    @JsonIgnore
    public List<ScmResourceFile> getUpdatedScmResourceFiles() {
        return getResourceFilesByOctaneStatus(OctaneStatus.MODIFIED);
    }

    public boolean isHasQuotedPaths() {
        return hasQuotedPaths;
    }

    public void setHasQuotedPaths(boolean hasQuotedPaths) {
        this.hasQuotedPaths = hasQuotedPaths;
    }


    public List<String> getDeletedFolders() {
        return deletedFolders;
    }

    public void setDeletedFolders(List<String> deletedFolders) {
        this.deletedFolders = deletedFolders;
    }

    @JsonProperty("tests")
    public List<AutomatedTest> getAllTests() {
        return tests;
    }

    @JsonProperty("tests")
    public void setAllTests(List<AutomatedTest> tests) {
        this.tests = tests;
    }

    @JsonProperty("dataTables")
    public List<ScmResourceFile> getAllScmResourceFiles() {
        return scmResourceFiles;
    }

    @JsonProperty("dataTables")
    public void setAllScmResourceFiles(List<ScmResourceFile> scmResourceFiles) {
        this.scmResourceFiles = scmResourceFiles;
    }

    public void writeToFile(File fileToWriteTo) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        writer.writeValue(fileToWriteTo, this);//write with UTF8

    }

    public static UftTestDiscoveryResult readFromFile(File file) throws IOException {
        Reader reader = new InputStreamReader(new FileInputStream(file), Charsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        UftTestDiscoveryResult result = mapper.readValue(reader, UftTestDiscoveryResult.class);
        return result;
    }

    public void sortItems() {
        sortTests(tests);
        sortDataTables(scmResourceFiles);
    }

    private static void sortTests(List<AutomatedTest> newTests) {
        Collections.sort(newTests, new Comparator<AutomatedTest>() {
            @Override
            public int compare(AutomatedTest o1, AutomatedTest o2) {
                int comparePackage = o1.getPackage().compareTo(o2.getPackage());
                if (comparePackage == 0) {
                    return o1.getName().compareTo(o2.getName());
                } else {
                    return comparePackage;
                }
            }
        });
    }

    private static void sortDataTables(List<ScmResourceFile> dataTables) {
        Collections.sort(dataTables, new Comparator<ScmResourceFile>() {
            @Override
            public int compare(ScmResourceFile o1, ScmResourceFile o2) {
                return o1.getRelativePath().compareTo(o2.getRelativePath());
            }
        });
    }

    public String getTestRunnerId() {
        return testRunnerId;
    }

    public void setTestRunnerId(String testRunnerId) {
        this.testRunnerId = testRunnerId;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public TestingToolType getTestingToolType() {
        return testingToolType;
    }

    public void setTestingToolType(TestingToolType testingToolType) {
        this.testingToolType = testingToolType;
    }

}