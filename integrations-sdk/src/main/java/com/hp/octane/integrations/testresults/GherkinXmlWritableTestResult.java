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
package com.hp.octane.integrations.testresults;

import com.hp.octane.integrations.dto.tests.TestRunResult;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class GherkinXmlWritableTestResult implements XmlWritableTestResult {
    private Map<String, String> attributes;
    private Element contentElement;
    private final int ERROR_MESSAGE_MAX_SIZE = System.getProperty("octane.sdk.tests.error_message_max_size") != null ? Integer.parseInt(System.getProperty("octane.sdk.tests.error_message_max_size")) : 512*512;

    public GherkinXmlWritableTestResult(String name, Element xmlElement, long duration, TestRunResult status) {
        this.attributes = new HashMap<>();
        this.attributes.put("name", name);
        this.attributes.put("duration", String.valueOf(duration));
        this.attributes.put("status", status.value());
        this.contentElement = xmlElement;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Element getXmlElement() {
        return contentElement;
    }

    public void writeXmlElement(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("gherkin_test_run");
        if (attributes != null) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                writer.writeAttribute(entry.getKey(), entry.getValue());
            }
        }
        writeXmlElement(writer, contentElement);
        writer.writeEndElement();
    }

    private void writeXmlElement(XMLStreamWriter writer, Element rootElement) throws XMLStreamException {
        if (rootElement != null) {
            writer.writeStartElement(rootElement.getTagName());
            for (int a = 0; a < rootElement.getAttributes().getLength(); a++) {
                String attrName = rootElement.getAttributes().item(a).getNodeName();
                writer.writeAttribute(attrName, rootElement.getAttribute(attrName));
            }
            NodeList childNodes = rootElement.getChildNodes();
            for (int c = 0; c < childNodes.getLength(); c++) {
                Node child = childNodes.item(c);
                if (child instanceof Element) {
                    writeXmlElement(writer, (Element) child);
                } else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
                    if(child.getParentNode() != null && "error_message".equals(child.getParentNode().getNodeName())){
                        String errorMassage = child.getNodeValue();
                        errorMassage = errorMassage.length() > ERROR_MESSAGE_MAX_SIZE ? StringUtils.abbreviate(errorMassage,ERROR_MESSAGE_MAX_SIZE) : errorMassage;
                        writer.writeCharacters(errorMassage);
                    } else {
                        writer.writeCharacters(child.getNodeValue());
                    }
                }
            }
            writer.writeEndElement();
        }
    }
}
