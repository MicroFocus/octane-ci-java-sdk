/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
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
package uftTest;

import com.hp.octane.integrations.uft.ufttestresults.UftTestResultsUtils;
import com.hp.octane.integrations.uft.ufttestresults.schema.UftResultStepData;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

public class RunResultsTestGetAggregatedError {

    @Test
    public void testOneError() {
        String err = getAggregatedError("run_results.xml");
        String expected = "Cannot identify the object \"rabbit\" (of class WebElement).";
        Assert.assertEquals(expected, err);
    }

    @Test
    public void testDuplicatedErrors() {
        String err = getAggregatedError("run_results_duplicatedErrors.xml");
        String expected = "The following add-in(s) were associated with your test, but are not currently loaded: WinForms, WPF. (Warning). \n" +
                "ActiveX component can't create object: 'WpfWindow'. ";
        Assert.assertEquals(expected, err);
    }

    @Test
    public void testResultForGUITestWithFail() {
        String err = getAggregatedError("run_results_GUITestWithFail.xml");
        String expected = "This step always fail. \n" +
                "This step always warn (Warning). ";
        Assert.assertEquals(expected, err);
    }

    @Test
    public void testResultForGUITestWithWarning() {
        String err = getAggregatedError("run_results_GUITestWithWarning.xml");
        String expected = "This step is always ends with warning (Warning). \n" +
                "This step is also  always ends with warning (Warning). ";
        Assert.assertEquals(expected, err);
    }

    @Test
    public void testResultForComputerLocked() {
        String err = getAggregatedError("run_results_computer_locked.xml");
        String expected = "The Micro Focus Unified Functional Testing computer is locked or logged off.";
        Assert.assertEquals(expected, err);
    }


    private String getAggregatedError(String fileName) {
        URL url = getClass().getResource(fileName);
        File path = new File(url.getPath());
        List<UftResultStepData> errorData = UftTestResultsUtils.getErrorData(path);
        return UftTestResultsUtils.getAggregatedErrorMessage(errorData);
    }
}
