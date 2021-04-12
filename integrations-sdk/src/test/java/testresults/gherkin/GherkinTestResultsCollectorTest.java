/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2021 Micro Focus or one of its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * ___________________________________________________________________
 */

package testresults.gherkin;

import com.hp.octane.integrations.testresults.GherkinUtils;
import com.hp.octane.integrations.testresults.GherkinXmlWritableTestResult;
import com.hp.octane.integrations.testresults.TestResultStatus;
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

    private List<File> getFilesFromFolder(String folder){
        return Arrays.asList(new File(getRootResource(folder,file0)),
                new File(getRootResource(folder,file1)));
    }

    @Test
    public void testConstruct() throws ParserConfigurationException, IOException, SAXException {
        GherkinUtils.parseFiles(getFilesFromFolder("f1"));
    }

    @Test
    public void testGetResults() throws ParserConfigurationException, IOException, SAXException {
        List<XmlWritableTestResult> gherkinTestsResults = GherkinUtils.parseFiles(getFilesFromFolder("f1"));
        Assert.assertEquals(3, gherkinTestsResults.size());
        validateGherkinTestResult((GherkinXmlWritableTestResult) gherkinTestsResults.get(0), "test Feature1", 21, TestResultStatus.FAILED);
        validateGherkinTestResult((GherkinXmlWritableTestResult) gherkinTestsResults.get(1), "test Feature10", 21, TestResultStatus.FAILED);
        validateGherkinTestResult((GherkinXmlWritableTestResult) gherkinTestsResults.get(2), "test Feature2", 21, TestResultStatus.PASSED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testXmlHasNoVersion() throws ParserConfigurationException, IOException, SAXException {
        GherkinUtils.parseFiles(Arrays.asList(new File(getRootResource("f2", file0))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testXmlHasHigherVersion() throws ParserConfigurationException, IOException, SAXException {
        GherkinUtils.parseFiles(Arrays.asList(new File(getRootResource("f3", file1))));
    }

    private void validateGherkinTestResult(GherkinXmlWritableTestResult gherkinTestResult, String name, long duration, TestResultStatus status) {
        validateAttributes(gherkinTestResult, name, duration, status);
        Assert.assertNotNull(gherkinTestResult.getXmlElement());
    }

    private void validateAttributes(GherkinXmlWritableTestResult gherkinTestResult, String name, long duration, TestResultStatus status) {
        Map<String, String> attributes = gherkinTestResult.getAttributes();
        Assert.assertEquals(name, attributes.get("name"));
        Assert.assertEquals(String.valueOf(duration), attributes.get("duration"));
        Assert.assertEquals(status.toPrettyName(), attributes.get("status"));
    }
}
