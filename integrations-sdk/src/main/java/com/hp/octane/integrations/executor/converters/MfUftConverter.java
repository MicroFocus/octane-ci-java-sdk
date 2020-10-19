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

import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.hp.octane.integrations.utils.SdkConstants;
import com.hp.octane.integrations.utils.SdkStringUtils;
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

/*
 * Converter to uft format (MTBX)
 */
public class MfUftConverter extends TestsToRunConverter {

    private static final Logger logger = LogManager.getLogger(MfUftConverter.class);
    public static final String DATA_TABLE_PARAMETER = "dataTable";
    public static final String ITERATIONS_PARAMETER = "iterations";

    public static final String INNER_RUN_ID_PARAMETER = "runId";//should not be handled by uft


    @Override
    public String convert(List<TestToRunData> data, String executionDirectory) {
        return convertToMtbxContent(data, executionDirectory);
    }

    public static String convertToMtbxContent(List<TestToRunData> tests, String workingDir) {
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
                    if (DATA_TABLE_PARAMETER.equals(paramKey) || ITERATIONS_PARAMETER.equals(paramKey) || INNER_RUN_ID_PARAMETER.equals(paramKey)) {
                        //skip, will be handled later
                    } else {
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
                });

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
}
