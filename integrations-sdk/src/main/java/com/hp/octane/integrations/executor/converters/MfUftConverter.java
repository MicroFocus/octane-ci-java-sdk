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

package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.hp.octane.integrations.utils.SdkConstants;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * Converter to uft format (MTBX)
 */
public class MfUftConverter extends TestsToRunConverter {

    private static final Logger logger = LogManager.getLogger(MfUftConverter.class);
    public static final String DATA_TABLE_PARAMETER = "dataTable";
    public static final String ITERATIONS_PARAMETER = "iterations";
    public static final String TESTING_TOOL_TYPE_PARAMETER = "testingToolType";
    public static final String MBT_DATA = "mbtData";

    public static final String INNER_RUN_ID_PARAMETER = "runId";//should not be handled by uft

    @Override
    public String convertInternal(List<TestToRunData> data, String executionDirectory, Map<String, String> globalParameters) {

        String myWorkingDir = executionDirectory;
        return convertToMtbxContent(data, myWorkingDir, globalParameters);
    }

    public String convertToMtbxContent(List<TestToRunData> tests, String workingDir, Map<String, String> globalParameters) {
        tests = tests.stream().filter(testToRunData -> testToRunData.getParameter(TESTING_TOOL_TYPE_PARAMETER) == null).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(tests)) {
            return "";
        }

        boolean addGlobalParameters = isAddGlobalParameters(globalParameters);

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
                        globalParameters.entrySet().stream().filter(p -> !p.getKey().equals(SdkConstants.JobParameters.ADD_GLOBAL_PARAMETERS_TO_TESTS_PARAM))
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

    private boolean isAddGlobalParameters(Map<String, String> globalParameters) {
        boolean addGlobalParameters = false;
        if (globalParameters != null) {
            //check job level params
            addGlobalParameters = globalParameters.containsKey(SdkConstants.JobParameters.ADD_GLOBAL_PARAMETERS_TO_TESTS_PARAM) &&
                    "true".equalsIgnoreCase(globalParameters.getOrDefault(SdkConstants.JobParameters.ADD_GLOBAL_PARAMETERS_TO_TESTS_PARAM, "false"));

            //check global param
            if (!addGlobalParameters && globalParameters.containsKey(SdkConstants.JobParameters.OCTANE_CONFIG_ID_PARAMETER_NAME)) {
                try {
                    OctaneClient octaneClient = OctaneSDK.getClientByInstanceId(globalParameters.get(SdkConstants.JobParameters.OCTANE_CONFIG_ID_PARAMETER_NAME));
                    addGlobalParameters = ConfigurationParameterFactory.addGlobalParametersToTests(octaneClient.getConfigurationService().getConfiguration());
                } catch (Exception e) {
                    logger.info("Failed to get octane client by id " + globalParameters.get(SdkConstants.JobParameters.OCTANE_CONFIG_ID_PARAMETER_NAME));
                }
            }
        }
        return addGlobalParameters;
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
}
