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

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.general.MbtAction;
import com.hp.octane.integrations.dto.general.MbtActionParameter;
import com.hp.octane.integrations.dto.general.MbtData;
import com.hp.octane.integrations.dto.general.MbtDataTable;
import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.hp.octane.integrations.executor.TestsToRunConverterResult;
import com.hp.octane.integrations.uft.UftTestDiscoveryUtils;
import com.hp.octane.integrations.utils.SdkConstants;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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

/*
 * Converter to uft format (MTBX)
 */
public class MfUftConverter extends TestsToRunConverter {

    private static final Logger logger = LogManager.getLogger(MfUftConverter.class);
    public static final String DATA_TABLE_PARAMETER = "dataTable";
    public static final String ITERATIONS_PARAMETER = "iterations";
    public static final String MBT_DATA = "mbtData";
    public static final String MBT_PARENT_SUB_DIR = "___mbt";
    List<MbtTest> mbtTests;

    public static final String INNER_RUN_ID_PARAMETER = "runId";//should not be handled by uft

    @Override
    public String convert(List<TestToRunData> data, String executionDirectory, Map<String, String> globalParameters) {

        String myWorkingDir = executionDirectory;
        if (isMBT(data)) {
            myWorkingDir = myWorkingDir + "\\" + MBT_PARENT_SUB_DIR;
            handleMBTModel(data, executionDirectory);

        }
        return convertToMtbxContent(data, myWorkingDir, globalParameters);
    }

    @Override
    protected void afterConvert(TestsToRunConverterResult result) {
        result.setMbtTests(mbtTests);
    }

    public String convertToMtbxContent(List<TestToRunData> tests, String workingDir, Map<String, String> globalParameters) {

        boolean addGlobalParameters = globalParameters != null;
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
                    globalParameters.entrySet().forEach(entry -> addParameterToTestElement(doc, testElement, entry.getKey(), entry.getValue()));
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

    private void handleMBTModel(List<TestToRunData> tests, String checkoutFolder) {

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

            String testLineSplitter = "'*************************************************************************************************************************************";
            scriptLinesList.add("");
            scriptLinesList.add(testLineSplitter);
            scriptLinesList.add("");

            Map<String, RecoveryScenario> recoveryScenariosMap = new LinkedHashMap<>();
            try {
                for (int i = 0; i < mbtData.getActions().size(); i++) {
                    MbtAction mbtAction = mbtData.getActions().get(i);

                    String actionParameters = extractActionParameterNames(mbtAction);
                    TestResources testResources = extractTestResources(mbtAction.getTestPath());
                    boolean theSameTestAsPrev = i > 0 ? mbtData.getActions().get(i - 1).getTestPath().equals(mbtAction.getTestPath()) : false;
                    boolean theSameTestAsNext = i < mbtData.getActions().size() - 1 ? mbtData.getActions().get(i + 1).getTestPath().equals(mbtAction.getTestPath()) : false;

                    if (theSameTestAsPrev) {
                        scriptLinesList.add("'Action belongs to test from previous action. Skip reloading function libraries and recovery scenarios");
                    } else {

                        if (!testResources.functionLibraries.isEmpty()) {
                            scriptLinesList.add("'Add function libraries");
                            scriptLinesList.add("RestartFLEngine");
                            for (String fl : testResources.functionLibraries) {
                                scriptLinesList.add(String.format("LoadFunctionLibrary \"%s\"", fl));
                            }
                        }

                        if (!testResources.recoveryScenarios.isEmpty()) {
                            scriptLinesList.add("");
                            scriptLinesList.add("'Activate recovery scenarios");
                            for (RecoveryScenario rs : testResources.recoveryScenarios) {
                                if (!recoveryScenariosMap.containsKey(rs.getKey())) {
                                    recoveryScenariosMap.put(rs.getKey(), rs);
                                    rs.position = recoveryScenariosMap.size();
                                }
                                RecoveryScenario myRS = recoveryScenariosMap.get(rs.getKey());

                                scriptLinesList.add(String.format("Recovery.SetScenarioStatus %s, True 'path=%s", myRS.getPosition(), myRS.path));
                            }
                        }
                    }

                    scriptLinesList.add("");
                    scriptLinesList.add("'Run action");
                    if (actionParameters != null) {
                        scriptLinesList.add(String.format("LoadAndRunAction \"%s\",\"%s\",rngAll%s", mbtAction.getTestPath(), mbtAction.getActionName(), actionParameters));
                    } else {
                        scriptLinesList.add(String.format("LoadAndRunAction \"%s\",\"%s\"", mbtAction.getTestPath(), mbtAction.getActionName()));
                    }

                    //disable recover scenarios
                    if (!theSameTestAsNext && !testResources.recoveryScenarios.isEmpty()) {
                        scriptLinesList.add("");
                        scriptLinesList.add("'De-active recovery scenarios");
                        for (RecoveryScenario rs : testResources.recoveryScenarios) {
                            RecoveryScenario myRS = recoveryScenariosMap.get(rs.getKey());
                            scriptLinesList.add(String.format("Recovery.SetScenarioStatus %s, False   'path=%s", myRS.getPosition(), myRS.path));
                        }
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

            //String script ="LoadAndRunAction \"c:\\Temp\\GUITest1\\\",\"Action1\"\r\nLoadAndRunAction \"c:\\Temp\\GUITest2\\\",\"Action1\"" ;
            //List<String> underlyingTests = Arrays.asList( "c:\\Temp\\GUITest6","c:\\Temp\\GUITest5");
            String script = String.join("\r\n", scriptLinesList);

            //ADD PARAMETERS to data table
            String encodedIterationsAsString = extractDataTableIterations(mbtData, data);

            List<String> recoveryScenarioList = recoveryScenariosMap.values().stream().sorted(Comparator.comparing(RecoveryScenario::getPosition))
                    .map(rs -> rs.asScriptLine()).collect(Collectors.toList());
            MbtTest test = new MbtTest(data.getTestName(), data.getPackageName(), script, underlyingTestsList, unitIds, encodedIterationsAsString,
                    Collections.emptyList(), recoveryScenarioList);
            mbtTests.add(test);
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
        int position;

        public static RecoveryScenario create(String path, String name) {
            RecoveryScenario rc = new RecoveryScenario();
            rc.path = path;
            rc.name = name;
            return rc;
        }

        public String asScriptLine() {
            return String.join(",", path, name, Integer.toString(getPosition()));
        }

        public String getKey() {
            return path + "_" + name;
        }

        public int getPosition() {
            return position;
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
