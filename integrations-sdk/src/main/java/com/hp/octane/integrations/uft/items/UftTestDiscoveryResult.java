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


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This file represents result of UFT discovery (tests and data tables)
 */
@XmlRootElement(name = "discoveryResult")
@XmlAccessorType(XmlAccessType.FIELD)
public class UftTestDiscoveryResult {


    @XmlElementWrapper(name = "tests")
    @XmlElement(name = "test")
    private List<AutomatedTest> tests = new ArrayList<>();

    @XmlElementWrapper(name = "dataTables")
    @XmlElement(name = "dataTable")
    private List<ScmResourceFile> scmResourceFiles = new ArrayList<>();

    @XmlElementWrapper(name = "deletedFolders")
    @XmlElement(name = "folder")
    private List<String> deletedFolders = new ArrayList<>();

    @XmlAttribute
    private String scmRepositoryId;

    @XmlAttribute
    private String workspaceId;

    @XmlAttribute
    private boolean fullScan;

    @XmlAttribute
    private boolean hasQuotedPaths;

    private List<AutomatedTest> getTestByOctaneStatus(OctaneStatus status) {
        List<AutomatedTest> filtered = new ArrayList<>();
        for (AutomatedTest test : tests) {
            if (test.getOctaneStatus().equals(status)) {
                filtered.add(test);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    private List<ScmResourceFile> getResourceFilesByOctaneStatus(OctaneStatus status) {
        List<ScmResourceFile> filtered = new ArrayList<>();
        for (ScmResourceFile file : scmResourceFiles) {
            if (file.getOctaneStatus().equals(status)) {
                filtered.add(file);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    public List<AutomatedTest> getNewTests() {
        return getTestByOctaneStatus(OctaneStatus.NEW);
    }

    public List<AutomatedTest> getDeletedTests() {
        return getTestByOctaneStatus(OctaneStatus.DELETED);
    }

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

    public boolean hasChanges() {
        return !getAllScmResourceFiles().isEmpty() || !getAllTests().isEmpty() || !getDeletedFolders().isEmpty();
    }

    public List<ScmResourceFile> getNewScmResourceFiles() {
        return getResourceFilesByOctaneStatus(OctaneStatus.NEW);
    }

    public List<ScmResourceFile> getDeletedScmResourceFiles() {
        return getResourceFilesByOctaneStatus(OctaneStatus.DELETED);
    }

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

    public List<AutomatedTest> getAllTests() {
        return tests;
    }

    public List<ScmResourceFile> getAllScmResourceFiles() {
        return scmResourceFiles;
    }

    public void writeToFile(File fileToWriteTo) throws JAXBException {

        JAXBContext jaxbContext = JAXBContext.newInstance(UftTestDiscoveryResult.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(this, fileToWriteTo);
    }

    public static UftTestDiscoveryResult readFromFile(File file) throws JAXBException, FileNotFoundException {
        JAXBContext context = JAXBContext.newInstance(UftTestDiscoveryResult.class);
        Unmarshaller m = context.createUnmarshaller();
        return (UftTestDiscoveryResult) m.unmarshal(new FileReader(file));
    }

    public void sortItems(){
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
}