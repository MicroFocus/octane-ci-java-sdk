/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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

package com.hp.octane.integrations.uft;

import org.apache.poi.poifs.filesystem.*;
import org.apache.poi.util.StringUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;

/**
 * Utility for extracting information from UFT test located on FS
 */
public class UftTestUtil {

    /**
     * Extract test description from UFT GUI test.
     * Note : UFT API test doesn't contain description
     *
     * @param dirPath path of UFT test
     * @return test description
     */
    public static String getTestDescription(File dirPath) {

        try {
            if (!dirPath.exists()) {
                return null;
            }

            File tspTestFile = new File(dirPath, "Test.tsp");
            if (!tspTestFile.exists()) {
                return null;
            }


            InputStream is = new FileInputStream(tspTestFile);
            String xmlContent = decodeXmlContent(is);


            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            //documentBuilderFactory.setIgnoringComments(true);
            //documentBuilderFactory.setIgnoringElementContentWhitespace(true);
            //documentBuilderFactory.setValidating(false);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(new InputSource(new StringReader(xmlContent)));
            String desc = document.getElementsByTagName("Description").item(0).getTextContent();
            return desc;
        } catch (Exception e) {
            return null;
        }
    }

    public static String decodeXmlContent(InputStream stream) throws IOException {
        POIFSFileSystem poiFS = new POIFSFileSystem(stream);
        DirectoryNode root = poiFS.getRoot();
        String xmlData = "";

        for (Entry entry : root) {
            String name = entry.getName();
            if ("ComponentInfo".equals(name)) {
                if (entry instanceof DirectoryEntry) {
                    System.out.println(entry);
                } else if (entry instanceof DocumentEntry) {
                    byte[] content = new byte[((DocumentEntry) entry).getSize()];
                    poiFS.createDocumentInputStream("ComponentInfo").read(content);
                    String fromUnicodeLE = StringUtil.getFromUnicodeLE(content);
                    xmlData = fromUnicodeLE.substring(fromUnicodeLE.indexOf('<')).replaceAll("\u0000", "");
                }
            }
        }
        return xmlData;
    }
}
