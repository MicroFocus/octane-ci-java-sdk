package uftTest;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.general.MbtUnitParameter;
import com.hp.octane.integrations.dto.general.MbtData;
import com.hp.octane.integrations.executor.converters.MfMBTConverter;
import com.hp.octane.integrations.uft.ufttestresults.UftTestResultsUtils;
import com.hp.octane.integrations.uft.ufttestresults.schema.UftResultIterationData;
import com.hp.octane.integrations.uft.ufttestresults.schema.UftResultStepData;
import com.hp.octane.integrations.uft.ufttestresults.schema.UftResultStepParameter;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class MbtTests {
    @Test
    public void parseConfiguration() {
        URL url = getClass().getResource("mbtExample1.json");
        MbtData mbtData = DTOFactory.getInstance().dtoFromJsonFile(new File(url.getFile()), MbtData.class);
        Assert.assertEquals(4, mbtData.getUnits().size());
        Assert.assertEquals(6, mbtData.getData().getParameters().size());
        Assert.assertEquals(2, mbtData.getData().getIterations().size());
        mbtData.getData().getIterations().forEach(strings -> Assert.assertEquals(6, strings.size()));
        mbtData.getUnits().forEach(mbtUnit -> System.out.println(mbtUnit.getName() + ", parameters: " +
                Optional.ofNullable(mbtUnit.getParameters()).orElse(Collections.emptyList()).stream().map(MbtUnitParameter::getParameterId).collect(Collectors.joining(", "))));
        mbtData.getData().getParameters().forEach(System.out::println);
        mbtData.getData().getIterations().forEach(strings -> strings.forEach(System.out::println));
    }

    @Test
    public void testComputeResourcePath() {
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase(Locale.ROOT).contains("windows")) {
            String path = MfMBTConverter.computeResourcePath("..\\ss", "c:\\aa\\bb");
            Assert.assertEquals("c:\\aa\\ss", path.toLowerCase(Locale.ROOT));
        }
    }

    @Test
    public void readActionResults1() {
        File file = new File(getClass().getResource("run_mbt_results_with_errors.xml").getFile());
        List<UftResultIterationData> resultData = UftTestResultsUtils.getMBTData(file);
        Assert.assertEquals(1, resultData.size());
        UftResultStepData data1 = resultData.get(0).getSteps().get(0);
        String errorMessage = "Cannot find the \"password\" object's parent \"Micro Focus MyFlight Sample\" (class WpfWindow).<br/>Verify that parent properties match an object currently displayed in your application.<br/><br/>Object's                                    physical description:<br>wpftypename = window<br>regexpwndtitle = Micro                                    Focus MyFlight Sample Application<br>devname = Micro Focus MyFlight Sample                                    Application<br> (Warning). ";
        validateAction(23, "Warning", errorMessage, "Action1 [Two test_same function 2]", data1, Collections.EMPTY_LIST, null);
    }

    @Test
    public void readActionResults2() {
        File file = new File(getClass().getResource("run_mbt_results_8_successful.xml").getFile());
        List<UftResultIterationData> iterations = UftTestResultsUtils.getMBTData(file);
        List<UftResultStepData> resultData = iterations.get(0).getSteps();
        Assert.assertEquals(8, resultData.size());

        List<UftResultStepParameter> inputParameters = Arrays.asList(new UftResultStepParameter("username", "john", "System.String"), new UftResultStepParameter("password", "HP", "System.String"));
        validateAction(1, "Done", "", "Launch App [FlightGUIBU2]", resultData.get(0), Collections.EMPTY_LIST, null);
        validateAction(3, "Done", "", "Login [FlightGUIBU2]", resultData.get(1), inputParameters, null);
        validateAction(1, "Done", "", "Search Order Tab [FlightGUIBU2]", resultData.get(2),  Collections.EMPTY_LIST, null);
        inputParameters = Arrays.asList(new UftResultStepParameter("Name", "john", "System.String"));
        validateAction(1, "Done", "", "Search Order By Name [FlightGUIBU2]", resultData.get(3), inputParameters, null);
        validateAction(0, "Done", "", "Select Order [FlightGUIBU2]", resultData.get(4), Collections.EMPTY_LIST, null);
        inputParameters = Arrays.asList(new UftResultStepParameter("NumSeats", "14", "System.String"), new UftResultStepParameter("Class", "Economy", "System.String"));
        validateAction(8, "Done", "", "Update Order Details [FlightGUIBU2]", resultData.get(5), inputParameters, null);
        inputParameters = Arrays.asList(new UftResultStepParameter("NumSeats", "10", "System.String"), new UftResultStepParameter("Class", "Economy", "System.String"));
        validateAction(7, "Done", "", "Update Order Details [FlightGUIBU2]", resultData.get(6), inputParameters, null);
        validateAction(0, "Done", "", "Close App [FlightGUIBU2]", resultData.get(7), Collections.EMPTY_LIST, null);
    }

    @Test
    public void readActionResults3() {
        File file = new File(getClass().getResource("run_mbt_results_with2_runs.xml").getFile());
        List<UftResultIterationData> iterations = UftTestResultsUtils.getMBTData(file);
        Assert.assertEquals(2, iterations.size());
        List<UftResultStepData> resultData1 = iterations.get(0).getSteps();
        List<UftResultStepData> resultData2 = iterations.get(1).getSteps();
        Assert.assertEquals(1, resultData1.size());
        Assert.assertEquals(1, resultData2.size());

        List<UftResultStepParameter> inputParameters = Arrays.asList(new UftResultStepParameter("parameter1", "4", "System.Double"), new UftResultStepParameter("parameter2", "2", "System.Double"));
        validateAction(11, "Done", "", "Action1 [FUNCTION-TEST1]", resultData1.get(0), inputParameters, null);
        inputParameters = Arrays.asList(new UftResultStepParameter("parameter1", "3", "System.Double"), new UftResultStepParameter("parameter2", "1", "System.Double"));
        validateAction(9, "Done", "", "Action1 [FUNCTION-TEST1]", resultData2.get(0), inputParameters, null);
    }

    private void validateAction(long duration, String result, String errorMessage, String lastParent, UftResultStepData data, List<UftResultStepParameter> inputParameters, List<UftResultStepParameter> outputParameters) {
        Assert.assertEquals(errorMessage, data.getMessage());
        Assert.assertEquals("Action", data.getType());
        Assert.assertEquals(duration, data.getDuration());
        Assert.assertEquals(result, data.getResult());
        Assert.assertEquals(3, data.getParents().size());
        Assert.assertEquals(lastParent, data.getParents().get(2));
        Assert.assertEquals(inputParameters, data.getInputParameters());
        Assert.assertEquals(outputParameters, data.getOutputParameters());
    }

    @Test
    public void testNameEncodingWithoutIllegalChars() {
        String name = "my name";
        String encoded = MfMBTConverter.encodeTestNameIfRequired(name);
        String decoded = MfMBTConverter.decodeTestNameIfRequired(encoded);
        Assert.assertEquals(name, decoded);
        Assert.assertEquals(name, encoded);
    }

    @Test
    public void testNameEncodingWithIllegalChars() {
        String name = "my name^*";
        String encoded = MfMBTConverter.encodeTestNameIfRequired(name);
        String decoded = MfMBTConverter.decodeTestNameIfRequired(encoded);
        Assert.assertEquals(name, decoded);
        Assert.assertNotEquals(name, encoded);
    }

    @Test
    public void testNameEncodingWithEndingSpace() {
        String name = "my name ";
        String encoded = MfMBTConverter.encodeTestNameIfRequired(name);
        String decoded = MfMBTConverter.decodeTestNameIfRequired(encoded);
        Assert.assertEquals(name, decoded);
        Assert.assertNotEquals(name, encoded);
    }


}
