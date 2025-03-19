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
import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.dto.general.MbtData;
import com.hp.octane.integrations.dto.general.MbtDataTable;
import com.hp.octane.integrations.dto.general.MbtUnit;
import com.hp.octane.integrations.dto.general.MbtUnitParameter;
import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverterResult;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.uft.UftTestDiscoveryUtils;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.commons.codec.Charsets;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.StringBuilderWriter;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hp.octane.integrations.services.rest.RestService.ACCEPT_HEADER;
import static com.hp.octane.integrations.utils.SdkConstants.JobParameters.*;

/*
 * Converter to uft format (MTBX)
 */
public class MfMBTConverter extends MfUftConverter {

    private static final Logger logger = LogManager.getLogger(MfMBTConverter.class);

    public static final String MBT_DATA = "mbtData";

    public static final String MBT_PARENT_SUB_DIR = "___mbt";

    public static final String MBT_DATA_NOT_INCLUDED = "mbtDataNotIncluded";

    public static final Pattern specialCharsPattern = Pattern.compile("[\\/:*?\"<>|%;]");

    public static final String BASE64_PREFIX = "BASE64_";

    public static final String INNER_RUN_ID_PARAMETER = "runId";//should not be handled by uft

    List<MbtTest> mbtTests;

    public static String encodeTestNameIfRequired(String name) {
        if (name.startsWith(" ") || name.endsWith(" ") || specialCharsPattern.matcher(name).find()) {
            return BASE64_PREFIX + Base64.getUrlEncoder().encodeToString(name.getBytes(Charsets.UTF_8));
        } else {
            return name;
        }
    }

    public static String decodeTestNameIfRequired(String name) {
        if (name.startsWith(BASE64_PREFIX)) {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(name.substring(BASE64_PREFIX.length()));
            return new String(decodedBytes, Charsets.UTF_8);
        } else {
            return name;
        }
    }

    private static boolean shouldRetrieveMbtData(List<TestToRunData> tests) {
        return tests.get(0).getParameters().get(MBT_DATA).equals(MBT_DATA_NOT_INCLUDED);
    }

    private static Map<String, String> parseSuiteRunDataJson(String responseJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper.readValue(responseJson, Map.class);
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
                        logger.error("Failed to build data table iterations record for mbt test {}", testToRunData.getTestName(), e);
                    }
                });
            } catch (IOException e) {
                logger.error("Failed to build data table iterations for mbt test {}", testToRunData.getTestName(), e);
            }
            String iterationsAsString = stringBuilderWriter.toString();
            byte[] encodedIterations = Base64.getEncoder().encode(iterationsAsString.getBytes(StandardCharsets.UTF_8));
            encodedIterationsAsString = new String(encodedIterations, StandardCharsets.UTF_8);
        }
        return encodedIterationsAsString;
    }

    private static String extractActionParameterNames(MbtUnit mbtUnit) {
        List<MbtUnitParameter> parameters = mbtUnit.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            StringBuilder inputParameters = new StringBuilder();
            StringBuilder outputParameters = new StringBuilder();
            parameters.forEach(mbtUnitParameter -> {
                String parameterName = mbtUnitParameter.getName();
                switch (mbtUnitParameter.getType().toUpperCase()) {
                    case "INPUT":
                        inputParameters.append(SdkStringUtils.isEmpty(mbtUnitParameter.getOutputParameter()) ?
                                ",DataTable(\"" + parameterName + "\")" : "," + mbtUnitParameter.getOutputParameter());
                        break;
                    case "OUTPUT":
                    default:
                        outputParameters.append(",").append(parameterName);
                        break;
                }
            });
            return inputParameters.append(outputParameters).toString();
        } else {
            return null;
        }
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

    @Override
    public String convertInternal(List<TestToRunData> data, String executionDirectory, Map<String, String> globalParameters) {
        String myWorkingDir = executionDirectory + "\\" + MBT_PARENT_SUB_DIR;
        handleMBTModel(data, executionDirectory, globalParameters);
        return super.convertToMtbxContent(data, myWorkingDir, globalParameters);
    }

    @Override
    protected void afterConvert(TestsToRunConverterResult result) {
        result.setMbtTests(mbtTests);
    }

    @Override
    public void enrichTestsData(List<TestToRunData> tests, Map<String, String> globalParameters) {
        handleMbtDataRetrieval(tests, globalParameters);
    }

    private void handleMBTModel(List<TestToRunData> tests, String checkoutFolder, Map<String, String> globalParameters) {
        //replace test name if required
        for (TestToRunData data : tests) {
            data.setTestName(encodeTestNameIfRequired(data.getTestName()));
        }

        mbtTests = new ArrayList<>();
        int order = 1;
        for (TestToRunData data : tests) {
            String mbtDataRaw = data.getParameter(MBT_DATA);
            MbtData mbtData;
            if (MBT_DATA_NOT_INCLUDED.equals(mbtDataRaw)) {
                throw new RuntimeException("Failed to fetch mbt data for test " + data.getTestName());
            }

            try {
                String raw = new String(Base64.getDecoder().decode(mbtDataRaw), StandardCharsets.UTF_8);
                mbtData = DTOFactory.getInstance().dtoFromJson(raw, MbtData.class);
                if(mbtData.getTestingToolType().equals(TestingToolType.UFT)) { // advance package name only for uft tests. it is not relevant for codeless tests
                    data.setPackageName("_" + order++);
                }
            } catch (Exception e) {
                String msg = "Failed to decode test action data " + data.getTestName() + " : " + e.getMessage();
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            if (mbtData.getTestingToolType().equals(TestingToolType.UFT)) {
                convertUftTest(checkoutFolder, data, mbtData);
            } else {
                convertCodelessTest(data, mbtData);
            }
        }
    }

    private void convertUftTest(String checkoutFolder, TestToRunData data, MbtData mbtData) {
        //parse test and action names
        for (int i = mbtData.getUnits().size() - 1; i >= 0; i--) {
            MbtUnit mbtUnit = mbtData.getUnits().get(i);
            String scmPath = mbtUnit.getPathInScm();
            if (scmPath == null || !scmPath.contains(":")) {
                logger.error("UnitId {} has invalid scmPath : {}, skipping", mbtUnit.getUnitId(), mbtUnit.getPathInScm());
                mbtData.getUnits().remove((i));
                continue;
            }
            String[] testAndActionParts = scmPath.split(":");//example : GUITests/f1/GUITest02:Action1
            String testPath = checkoutFolder + "\\" + testAndActionParts[0];
            String actionName = testAndActionParts[1];
            mbtUnit.setTestPath(testPath).setActionName(actionName);
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
            for (int i = 0; i < mbtData.getUnits().size(); i++) {
                MbtUnit mbtUnit = mbtData.getUnits().get(i);
                String sanitizedTestPath = sanitizeTestPath(mbtUnit.getTestPath()); // remove the action from the test path is exists
                mbtUnit.setTestPath(sanitizedTestPath);

                //validate test folder exist
                File testFolderFile = new File(mbtUnit.getTestPath());
                if (!testFolderFile.exists()) {
                    throw new IllegalArgumentException(String.format("Test path %s is not found. UnitId = %s.", mbtUnit.getTestPath(), mbtUnit.getUnitId()));
                }
                //validate that uft test folder is valid
                if (UftTestDiscoveryUtils.isUftTestFolder(testFolderFile.listFiles()).isNone()) {
                    throw new IllegalArgumentException(String.format("Invalid test path %s. UnitId = %s.", mbtUnit.getTestPath(), mbtUnit.getUnitId()));
                }

                String actionParameters = extractActionParameterNames(mbtUnit);
                TestResources testResources = extractTestResources(mbtUnit.getTestPath());
                boolean theSameTestAsPrev = i > 0 ? mbtData.getUnits().get(i - 1).getTestPath().equals(mbtUnit.getTestPath()) : false;

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
                    scriptLinesList.add(String.format("LoadAndRunAction \"%s\",\"%s\",rngAll%s", mbtUnit.getTestPath(), mbtUnit.getActionName(), actionParameters));
                } else {
                    scriptLinesList.add(String.format("LoadAndRunAction \"%s\",\"%s\"", mbtUnit.getTestPath(), mbtUnit.getActionName()));
                }
                scriptLinesList.add("\r\nIf Reporter.CurrentActionIterationStatus = 1 Then\r\nExitAction\r\nEnd If");

                scriptLinesList.add("");
                scriptLinesList.add(testLineSplitter);
                scriptLinesList.add("");

                //END SCRIPT

                underlyingTestsList.add(mbtUnit.getTestPath());
                unitIds.add(mbtUnit.getUnitId());
            }

        } catch (Exception e) {
            logger.error("Failed to build script for test {} : {}", data.getTestName(), e.getMessage());
            throw e;
        }

        String script = String.join("\r\n", scriptLinesList);

        //ADD PARAMETERS to data table
        String encodedIterationsAsString = extractDataTableIterations(mbtData, data);

        MbtUftTest test = new MbtUftTest(data.getTestName(), data.getPackageName(), script, underlyingTestsList, unitIds, encodedIterationsAsString,
                Collections.emptyList(), Collections.emptyList());
        mbtTests.add(test);
    }

    private void convertCodelessTest(TestToRunData data, MbtData mbtData) {
        // this will prevent codeless test to be transformed to mtbx
        data.addParameters(TESTING_TOOL_TYPE_PARAMETER, TestingToolType.CODELESS.value());

        MbtCodelessTest mbtCodelessTest = new MbtCodelessTest(data.getTestName());
        mbtCodelessTest.setMbtDataTable(mbtData.getData());
        for (MbtUnit mbtUnit : mbtData.getUnits()) {
            MbtCodelessUnit mbtCodelessUnit = new MbtCodelessUnit();
            mbtCodelessUnit.setUnitId(mbtUnit.getUnitId());
            mbtCodelessUnit.setName(mbtUnit.getName());
            mbtCodelessUnit.setOrder(mbtUnit.getOrder());
            mbtCodelessUnit.setScript(mbtUnit.getScript());
            mbtCodelessUnit.setParameters(mbtUnit.getParameters());

            mbtCodelessTest.getUnits().add(mbtCodelessUnit);
        }

        mbtTests.add(mbtCodelessTest);
    }

    // remove action# from repository path for execution, if exists
    private String sanitizeTestPath(String testPath) {
        if (SdkStringUtils.isEmpty(testPath)) {
            return testPath;
        }

        int lastIndex = testPath.lastIndexOf("\\");
        if (lastIndex > -1) {
            String lastPart = testPath.substring(lastIndex);
            if (lastPart.toLowerCase().contains("action")) {
                return testPath.substring(0, lastIndex);
            }
        }

        return testPath;
    }

    private void handleMbtDataRetrieval(List<TestToRunData> tests, Map<String, String> globalParameters) {
        if (shouldRetrieveMbtData(tests)) {
            OctaneClient octaneClient = OctaneSDK.getClientByInstanceId(globalParameters.get(OCTANE_CONFIG_ID_PARAMETER_NAME));
            OctaneConfiguration octaneConfig = octaneClient.getConfigurationService().getConfiguration();
            String url = octaneConfig.getUrl() + "/api" + "/shared_spaces/" + octaneConfig.getSharedSpace() +
                    "/workspaces/" + globalParameters.get(OCTANE_WORKSPACE_PARAMETER_NAME) +
                    "/suite_runs/" + globalParameters.get(SUITE_RUN_ID_PARAMETER_NAME) + "/get_suite_data";

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
                } else {
                    logger.error("Failed to get response {}", (octaneResponse != null ? octaneResponse.getStatus() : "(null)"));
                }
            } catch (IOException e) {
                logger.error("Failed to get response ", e);
            }
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
                    content.recoveryScenarios.add(RecoveryScenario.create(rsPath, rsName));
                }
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            logger.error("Failed to parse function libraries/recovery scenarios for tests {} : {}", testPath, e);
        }
        return content;
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

}
