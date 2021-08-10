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

package com.hp.octane.integrations.executor.converters;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.general.MbtAction;
import com.hp.octane.integrations.dto.general.MbtActionParameter;
import com.hp.octane.integrations.dto.general.MbtData;
import com.hp.octane.integrations.dto.general.MbtDataTable;
import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.hp.octane.integrations.executor.TestsToRunConverterResult;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.uft.UftTestDiscoveryUtils;
import com.hp.octane.integrations.utils.SdkConstants;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.StringBuilderWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.hp.octane.integrations.services.rest.RestService.ACCEPT_HEADER;
import static com.hp.octane.integrations.utils.SdkConstants.JobParameters.*;

/*
 * Converter to uft format (MTBX)
 */
public class MfUftConverter extends TestsToRunConverter {

    private static final Logger logger = LogManager.getLogger(MfUftConverter.class);
    public static final String DATA_TABLE_PARAMETER = "dataTable";
    public static final String ITERATIONS_PARAMETER = "iterations";
    public static final String MBT_DATA = "mbtData";
    public static final String RUN_ID = "runId";
    public static final String MBT_PARENT_SUB_DIR = "___mbt";
    public static final String MBT_DATA_NOT_INCLUDED = "mbtDataNotIncluded";

    List<MbtTest> mbtTests;

    public static final String INNER_RUN_ID_PARAMETER = "runId";//should not be handled by uft

    @Override
    public String convert(List<TestToRunData> data, String executionDirectory, Map<String, String> globalParameters) {

        String myWorkingDir = executionDirectory;
        if (isMBT(data)) {
            myWorkingDir = myWorkingDir + "\\" + MBT_PARENT_SUB_DIR;
            handleMBTModel(data, executionDirectory, globalParameters);

        }
        return convertToMtbxContent(data, myWorkingDir, globalParameters);
    }

    @Override
    protected void afterConvert(TestsToRunConverterResult result) {
        result.setMbtTests(mbtTests);
    }

    public String convertToMtbxContent(List<TestToRunData> tests, String workingDir, Map<String, String> globalParameters) {

        boolean addGlobalParameters = globalParameters != null &&
                globalParameters.containsKey(SdkConstants.JobParameters.ADD_GLOBAL_PARAMETERS_TO_TESTS_PARAM) &&
                "true".equalsIgnoreCase(globalParameters.getOrDefault(SdkConstants.JobParameters.ADD_GLOBAL_PARAMETERS_TO_TESTS_PARAM,"false"));
        /*<Mtbx>
            <Test name="test1" path=workingDir + "\APITest1">
            <Parameter type="string" name="myName" value="myValue"/> //type is optional, possible values = float,string,any,boolean,bool,int,integer,number,password,datetime,date,long,double,decimal
			<DataTable path=workingDir+"\aa\bbb.xslx"/>
			<Iterations mode="rngIterations|rngAll|oneIteration" start="2" end="3"/>
			 ….
			</Test>
			<Test name="test2" path=workingDir+"\${CHECKOUT_SUBDIR}\test2">

			….
			</Test>
		</Mtbx>*/

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("Mtbx");
            doc.appendChild(rootElement);

            for (TestToRunData test : tests) {
                Element testElement = doc.createElement("Test");
                String packageAndTestName = (SdkStringUtils.isNotEmpty(test.getPackageName())
                        ? test.getPackageName() + SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER
                        : "")
                        + test.getTestName();
                testElement.setAttribute("name", packageAndTestName);
                String path = workingDir + (SdkStringUtils.isEmpty(test.getPackageName())
                        ? ""
                        : SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER + test.getPackageName())
                        + SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER + test.getTestName();
                testElement.setAttribute("path", path);

                //add parameters
                test.getParameters().forEach((paramKey, paramValue) -> {
                    if (DATA_TABLE_PARAMETER.equals(paramKey) || ITERATIONS_PARAMETER.equals(paramKey) || MBT_DATA.equals(paramKey)) {
                        //skip, will be handled later
                    } else {
                        if (INNER_RUN_ID_PARAMETER.equals(paramKey)) {
                            if (!addGlobalParameters) {
                                return;
                            }
                        }

                        addParameterToTestElement(doc, testElement, paramKey, paramValue);
                    }
                });
                if (addGlobalParameters) {
                    globalParameters.entrySet().stream().filter(p->!p.getKey().equals(SdkConstants.JobParameters.ADD_GLOBAL_PARAMETERS_TO_TESTS_PARAM))
                            .forEach(entry -> addParameterToTestElement(doc, testElement, entry.getKey(), entry.getValue()));
                }

                //add data table
                String dataTable = test.getParameter(DATA_TABLE_PARAMETER);
                if (SdkStringUtils.isNotEmpty(dataTable)) {
                    Element dataTableElement = doc.createElement("DataTable");
                    dataTableElement.setAttribute("path", workingDir + SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER + dataTable);
                    testElement.appendChild(dataTableElement);
                }

                //add iterations
                String iterations = test.getParameter(ITERATIONS_PARAMETER);
                if (SdkStringUtils.isNotEmpty(iterations)) {
                    String[] parts = iterations.split(",");
                    Element iterationElement = doc.createElement("Iterations");
                    iterationElement.setAttribute("mode", parts[0].trim());

                    if (parts.length >= 3) {
                        iterationElement.setAttribute("start", parts[1].trim());
                        iterationElement.setAttribute("end", parts[2].trim());
                    }
                    testElement.appendChild(iterationElement);
                }


                rootElement.appendChild(testElement);
            }

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            return writer.toString();
        } catch (ParserConfigurationException | TransformerException e) {
            String msg = "Failed to build MTBX content : " + e.getMessage();
            logger.error(msg);
            throw new RuntimeException(msg);
        }
    }

    private void addParameterToTestElement(Document doc, Element testElement, String paramKey, String paramValue) {
        Element parameterElement = doc.createElement("Parameter");
        parameterElement.setAttribute("name", paramKey);
        if (paramValue != null && paramValue.startsWith("(")) {
            //example : (float)actualParamValue
            int endIndex = paramValue.indexOf(")");
            if (endIndex != -1) {
                String type = paramValue.substring(1/*skip first (*/, endIndex).trim();

                String value = "";
                if (paramValue.length() >= (endIndex + 1)) {
                    value = paramValue.substring(endIndex + 1).trim();
                }

                parameterElement.setAttribute("value", value);
                parameterElement.setAttribute("type", type);
            }
        } else {
            parameterElement.setAttribute("value", paramValue);
        }

        testElement.appendChild(parameterElement);
    }

    private static boolean isMBT(List<TestToRunData> tests) {
        return tests.get(0).getParameter(MBT_DATA) != null;
    }

    private void handleMBTModel(List<TestToRunData> tests, String checkoutFolder, Map<String, String> globalParameters) {
        if (shouldRetrieveMbtData(tests)){
            OctaneClient octaneClient = OctaneSDK.getClientByInstanceId(globalParameters.get(OCTANE_CONFIG_ID_PARAMETER_NAME));
            OctaneConfiguration octaneConfig = octaneClient.getConfigurationService().getConfiguration();
            String url = octaneConfig.getUrl() + "/api" + "/shared_spaces/" + octaneConfig.getSharedSpace() +
                        "/workspaces/" + globalParameters.get(OCTANE_WORKSPACE_PARAMETER_NAME) +
                        "/suite_runs/" + globalParameters.get(SUITE_RUN_ID_PARAMETER_NAME) + "/get_suite_data" ;

            Map<String, String> headers = new HashMap<>();
            headers.put(ACCEPT_HEADER, ContentType.APPLICATION_JSON.getMimeType());
            headers.put(OctaneRestClient.CLIENT_TYPE_HEADER, OctaneRestClient.CLIENT_TYPE_VALUE);

            OctaneRequest request = DTOFactory.getInstance()
                    .newDTO(OctaneRequest.class)
                    .setMethod(HttpMethod.GET)
                    .setHeaders(headers)
                    .setUrl(url);

            try {
                OctaneResponse octaneResponse = octaneClient.getRestService().obtainOctaneRestClient().execute(request);
                if (octaneResponse != null && octaneResponse.getStatus() == HttpStatus.SC_OK) {
                    Map<String, String> parsedResponse = parseSuiteRunDataJson(octaneResponse.getBody());
                    if (parsedResponse != null) {
                        for (TestToRunData test : tests) {
                            String runID = test.getParameter(INNER_RUN_ID_PARAMETER);
                            test.addParameters(MBT_DATA, parsedResponse.get(runID));
                        }
                    }
                }
                else{
                    logger.error("Failed to get response "+ (octaneResponse != null ? octaneResponse.getStatus() : "(null)") );
                    return;
                }
            } catch (IOException e) {
                logger.error("Failed to get response ", e);
                return;
            }
        }
        mbtTests = new ArrayList<>();
        //StringBuilder str = new StringBuilder();
        int order = 1;
        for (TestToRunData data : tests) {
            data.setPackageName("_" + order++);
            String mbtDataRaw = data.getParameter(MBT_DATA);
            MbtData mbtData;
            try {
                String raw = new String(Base64.getDecoder().decode(mbtDataRaw), StandardCharsets.UTF_8);
                mbtData = DTOFactory.getInstance().dtoFromJson(raw, MbtData.class);
            } catch (Exception e) {
                logger.error("Failed to decode test action data " + data.getTestName() + " : " + e.getMessage());
                throw e;
            }
            //parse test and action names
            for (int i = mbtData.getActions().size() - 1; i >= 0; i--) {
                MbtAction mbtAction = mbtData.getActions().get(i);
                String scmPath = mbtAction.getPathInScm();
                if (scmPath == null || !scmPath.contains(":")) {
                    logger.error(String.format("UnitId %s has invalid scmPath : %s, skipping", mbtAction.getUnitId(), mbtAction.getPathInScm()));
                    mbtData.getActions().remove((i));
                    continue;
                }
                String[] testAndActionParts = scmPath.split(":");//example : GUITests/f1/GUITest02:Action1
                String testPath = checkoutFolder + "\\" + testAndActionParts[0];
                String actionName = testAndActionParts[1];
                mbtAction.setTestPath(testPath).setActionName(actionName);
            }

            //build script
            List<String> scriptLinesList = new ArrayList<>();
            List<String> underlyingTestsList = new ArrayList<>();
            List<Long> unitIds = new ArrayList<>();

            String testLineSplitter = "'********************************************************************************************************************************************";
            scriptLinesList.add("");
            scriptLinesList.add(testLineSplitter);
            scriptLinesList.add("");

            try {
                for (int i = 0; i < mbtData.getActions().size(); i++) {
                    MbtAction mbtAction = mbtData.getActions().get(i);
                    if (!new File(mbtAction.getTestPath()).exists()) {
                        throw new IllegalArgumentException(String.format("Test path %s is not found. UnitId = %s.", mbtAction.getTestPath(), mbtAction.getUnitId()));
                    }

                    String actionParameters = extractActionParameterNames(mbtAction);
                    TestResources testResources = extractTestResources(mbtAction.getTestPath());
                    boolean theSameTestAsPrev = i > 0 ? mbtData.getActions().get(i - 1).getTestPath().equals(mbtAction.getTestPath()) : false;

                    if (theSameTestAsPrev) {
                        scriptLinesList.add("'The action belongs to the test of the previous action. Skip reloading function libraries and recovery scenarios.");
                    } else {

                        if (!testResources.functionLibraries.isEmpty()) {
                            scriptLinesList.add("");
                            scriptLinesList.add("'Add function libraries");
                            scriptLinesList.add("RestartFLEngine");
                            for (String fl : testResources.functionLibraries) {
                                scriptLinesList.add(String.format("LoadFunctionLibrary \"%s\"", fl));
                            }
                        }

                        if (!testResources.recoveryScenarios.isEmpty()) {
                            scriptLinesList.add("");
                            scriptLinesList.add("'Add recovery scenarios");
                            scriptLinesList.add("CleanRSManager");
                            String scenarios = "LoadRecoveryScenario " + testResources.recoveryScenarios.stream().
                                    map(rs -> String.format("\"%s|%s|1|1*\"", rs.path, rs.name)).collect(Collectors.joining(","));
                            scriptLinesList.add(scenarios);
                        }
                    }

                    scriptLinesList.add("");
                    scriptLinesList.add("'Run action");
                    if (actionParameters != null) {
                        scriptLinesList.add(String.format("LoadAndRunAction \"%s\",\"%s\",rngAll%s", mbtAction.getTestPath(), mbtAction.getActionName(), actionParameters));
                    } else {
                        scriptLinesList.add(String.format("LoadAndRunAction \"%s\",\"%s\"", mbtAction.getTestPath(), mbtAction.getActionName()));
                    }

                    scriptLinesList.add("");
                    scriptLinesList.add(testLineSplitter);
                    scriptLinesList.add("");

                    //END SCRIPT

                    underlyingTestsList.add(mbtAction.getTestPath());
                    unitIds.add(mbtAction.getUnitId());
                }

            } catch (Exception e) {
                logger.error("Failed to build script for test " + data.getTestName() + " : " + e.getMessage());
                throw e;
            }


            String script = String.join("\r\n", scriptLinesList);

            //ADD PARAMETERS to data table
            String encodedIterationsAsString = extractDataTableIterations(mbtData, data);

            MbtTest test = new MbtTest(data.getTestName(), data.getPackageName(), script, underlyingTestsList, unitIds, encodedIterationsAsString,
                    Collections.emptyList(), Collections.emptyList());
            mbtTests.add(test);
        }
    }

    private static boolean shouldRetrieveMbtData(List<TestToRunData> tests){
        return tests.get(0).getParameters().get(MBT_DATA).equals(MBT_DATA_NOT_INCLUDED);
    }

    private static Map<String, String> parseSuiteRunDataJson(String responseJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Map<String, String> result = objectMapper.readValue(responseJson, Map.class);
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid suite run data format: " + e.getMessage(), e);
        }
    }

    private static String extractDataTableIterations(MbtData mbtData, TestToRunData testToRunData) {
        String encodedIterationsAsString = "";
        MbtDataTable dataTable = mbtData.getData();
        if (dataTable != null && dataTable.getParameters() != null && !dataTable.getParameters().isEmpty()) {
            StringBuilderWriter stringBuilderWriter = new StringBuilderWriter();
            try (CSVPrinter csvPrinter = new CSVPrinter(stringBuilderWriter, CSVFormat.DEFAULT.withHeader(dataTable.getParameters().stream().toArray(String[]::new)))) {
                dataTable.getIterations().forEach(iteration -> {
                    try {
                        csvPrinter.printRecord(iteration);
                    } catch (IOException e) {
                        logger.error("Failed to build data table iterations record for mbt test " + testToRunData.getTestName(), e);
                    }
                });
            } catch (IOException e) {
                logger.error("Failed to build data table iterations for mbt test " + testToRunData.getTestName(), e);
            }
            String iterationsAsString = stringBuilderWriter.toString();
            byte[] encodedIterations = Base64.getEncoder().encode(iterationsAsString.getBytes(StandardCharsets.UTF_8));
            encodedIterationsAsString = new String(encodedIterations, StandardCharsets.UTF_8);
        }
        return encodedIterationsAsString;
    }

    private static String extractActionParameterNames(MbtAction mbtAction) {
        List<MbtActionParameter> parameters = mbtAction.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            StringBuilder parameterString = new StringBuilder();
            parameters.forEach(mbtActionParameter -> {
                String parameterName = mbtActionParameter.getName();
                switch (mbtActionParameter.getType().toUpperCase()) {
                    case "INPUT":
                        parameterString.append(SdkStringUtils.isEmpty(mbtActionParameter.getOutputParameter()) ?
                                ",DataTable(\"" + parameterName + "\")" : "," + mbtActionParameter.getOutputParameter());
                        break;
                    case "OUTPUT":
                    default:
                        parameterString.append(",").append(parameterName);
                        break;
                }
            });
            return parameterString.toString();
        } else {
            return null;
        }
    }

    private static class TestResources {
        List<String> functionLibraries = new ArrayList<>();
        List<RecoveryScenario> recoveryScenarios = new ArrayList<>();
    }

    private static class RecoveryScenario {
        String path;
        String name;

        public static RecoveryScenario create(String path, String name) {
            RecoveryScenario rc = new RecoveryScenario();
            rc.path = path;
            rc.name = name;
            return rc;
        }
    }

    private TestResources extractTestResources(String testPath) {
        TestResources content = new TestResources();
        //ADD function libraries and recovery scenarios
        try {
            File tspFile = new File(testPath + "\\Test.tsp");
            InputStream is = new FileInputStream(tspFile);
            String xmlContent = UftTestDiscoveryUtils.extractXmlContentFromTspFile(is);

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(new InputSource(new StringReader(xmlContent)));

            //FL
            NodeList funcLibNodes = document.getElementsByTagName("FuncLib");
            for (int i = 0; i < funcLibNodes.getLength(); i++) {
                String fl = document.getElementsByTagName("FuncLib").item(i).getTextContent();
                content.functionLibraries.add(computeResourcePath(fl, testPath));
            }

            //RC
            String[] recoveryScenariosParts = document.getElementsByTagName("RecoveryScenarios").item(0).getTextContent().split("\\*");
            for (String recoveryScenariosPart : recoveryScenariosParts) {
                String[] rsAsArray = recoveryScenariosPart.split("\\|");
                if (rsAsArray.length > 1) {
                    String rsPath = computeResourcePath(rsAsArray[0], testPath);
                    String rsName = rsAsArray[1];
                    //String position = rsAsArray[2];
                    content.recoveryScenarios.add(RecoveryScenario.create(rsPath, rsName));
                }
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            logger.error("Failed to parse function libraries/recovery scenarios for tests " + testPath + " : " + e);
        }
        return content;
    }

    /**
     * if path to resource is relative to test, compute absolute path to resource
     *
     * @param testPath
     * @param resourcePath
     * @return
     */
    public static String computeResourcePath(String resourcePath, String testPath) {
        if (resourcePath.startsWith("..")) {
            File file = new File(testPath, resourcePath);
            try {
                return file.getCanonicalPath();
            } catch (IOException e) {
                String msg = String.format("Failed to computeResourcePath for resource %s , test %s", resourcePath, testPath);
                logger.error(msg);
                return resourcePath;
            }
        }
        return resourcePath;

    }
}
