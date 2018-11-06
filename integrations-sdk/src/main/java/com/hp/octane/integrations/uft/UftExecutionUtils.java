package com.hp.octane.integrations.uft;

import com.hp.octane.integrations.dto.executor.TestExecutionInfo;
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

public class UftExecutionUtils {

	private final static Logger logger = LogManager.getLogger(UftExecutionUtils.class);

	public static String convertToMtbxContent(List<TestExecutionInfo> tests, String workingDir) {
        /*<Mtbx>
            <Test name="test1" path=workingDir + "\APITest1">
			<DataTable path=workingDir+"\aa\bbb.xslx"/>
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

			for (TestExecutionInfo test : tests) {
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

				if (SdkStringUtils.isNotEmpty(test.getDataTable())) {
					Element dataTableElement = doc.createElement("DataTable");
					dataTableElement.setAttribute("path", workingDir + SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER + test.getDataTable());
					testElement.appendChild(dataTableElement);
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
