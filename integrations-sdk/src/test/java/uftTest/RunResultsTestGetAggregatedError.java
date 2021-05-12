package uftTest;

import com.hp.octane.integrations.uft.ufttestresults.UftTestResultsUtils;
import com.hp.octane.integrations.uft.ufttestresults.schema.UftErrorData;
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

    private String getAggregatedError(String fileName) {
        URL url = getClass().getResource(fileName);
        File path = new File(url.getPath());
        List<UftErrorData> errorData = UftTestResultsUtils.getErrorData(path);
        return UftTestResultsUtils.getAggregatedErrorMessage(errorData);
    }
}
