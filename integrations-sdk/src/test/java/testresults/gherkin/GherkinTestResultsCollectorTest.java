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

package testresults.gherkin;

import com.hp.octane.integrations.testresults.GherkinUtils;
import com.hp.octane.integrations.testresults.GherkinXmlWritableTestResult;
import com.hp.octane.integrations.testresults.XmlWritableTestResult;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GherkinTestResultsCollectorTest {

    String file0 = "OctaneGherkinResults0.xml";
    String file1 = "OctaneGherkinResults1.xml";


    private String getRootResource(String resourceRelativePath, String resourceName) {
        String resource;
        if (resourceRelativePath.isEmpty()) {
            resource = resourceName;
        } else {
            resource = resourceRelativePath + "/" + resourceName;
        }
        URL url = getClass().getResource(resource);
        String path = url.getPath();
        return path;
    }

    private List<File> getFilesFromFolder(String folder) {
        return Arrays.asList(new File(getRootResource(folder, file0)),
                new File(getRootResource(folder, file1)));
    }

    @Test
    public void testConstruct() throws ParserConfigurationException, IOException, SAXException {
        GherkinUtils.parseFiles(getFilesFromFolder("f1"));
    }

    @Test
    public void testGetResults() throws ParserConfigurationException, IOException, SAXException {
        List<XmlWritableTestResult> gherkinTestsResults = GherkinUtils.parseFiles(getFilesFromFolder("f1"));
        Assert.assertEquals(3, gherkinTestsResults.size());
        validateGherkinTestResult((GherkinXmlWritableTestResult) gherkinTestsResults.get(0), "test Feature1", 21, "Failed");
        validateGherkinTestResult((GherkinXmlWritableTestResult) gherkinTestsResults.get(1), "test Feature10", 21, "Failed");
        validateGherkinTestResult((GherkinXmlWritableTestResult) gherkinTestsResults.get(2), "test Feature2", 21, "Passed");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testXmlHasNoVersion() throws ParserConfigurationException, IOException, SAXException {
        GherkinUtils.parseFiles(Arrays.asList(new File(getRootResource("f2", file0))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testXmlHasHigherVersion() throws ParserConfigurationException, IOException, SAXException {
        GherkinUtils.parseFiles(Arrays.asList(new File(getRootResource("f3", file1))));
    }

    @Test
    public void testTemplateWithCounter() {
        String folder = new File(getRootResource("f3", file0)).getParent();
        List<File> files = GherkinUtils.findGherkinFilesByTemplateWithCounter(folder, "OctaneGherkinResults%s.xml", 0);
        Assert.assertEquals(file0, files.get(0).getName());
        Assert.assertEquals(file1, files.get(1).getName());
    }

    private void validateGherkinTestResult(GherkinXmlWritableTestResult gherkinTestResult, String name, long duration, String status) {
        validateAttributes(gherkinTestResult, name, duration, status);
        Assert.assertNotNull(gherkinTestResult.getXmlElement());
    }

    private void validateAttributes(GherkinXmlWritableTestResult gherkinTestResult, String name, long duration, String status) {
        Map<String, String> attributes = gherkinTestResult.getAttributes();
        Assert.assertEquals(name, attributes.get("name"));
        Assert.assertEquals(String.valueOf(duration), attributes.get("duration"));
        Assert.assertEquals(status, attributes.get("status"));
    }
}
