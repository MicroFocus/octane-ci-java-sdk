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
 */

package com.hp.octane.integrations.uft;

import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.uft.items.*;
import com.hp.octane.integrations.utils.SdkConstants;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.poifs.filesystem.*;
import org.apache.poi.util.StringUtil;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UftTestDiscoveryUtils {

    private static final Logger logger = LogManager.getLogger(UftTestDiscoveryUtils.class);

    private static final String STFileExtention = ".st";//api test

    private static final String QTPFileExtention = ".tsp";//gui test

    private static final String XLSXExtention = ".xlsx";//excel file

    private static final String XLSExtention = ".xls";//excel file

    private static final String GUI_TEST_FILE = "test" + QTPFileExtention;//gui test

    private static final String API_ACTIONS_FILE = "actions.xml";//api test

    private static final String RESOURCE_MTR_FILE = "resource.mtr";//parameters file

    private static final Set<String> SKIP_FOLDERS = Stream.of("_discovery_results").collect(Collectors.toSet());

    public static final String ACTION_0 = "action0";

    public static final String UFT_PARAM_ARG_DEFAULT_VALUE_NODE_NAME = "ArgDefaultValue";

    public static final String UFT_PARAM_ARG_NAME_NODE_NAME = "ArgName";

    public static final String UFT_PARAM_ARGUMENTS_COLLECTION_NODE_NAME = "ArgumentsCollection";

    public static final String UFT_COMPONENT_NODE_NAME = "Component";

    public static final String UFT_DEPENDENCY_NODE_NAME = "Dependency";

    public static final String UFT_ACTION_LOGICAL_ATTR = "Logical";

    public static final String UFT_ACTION_KIND_ATTR = "Kind";

    public static final String UFT_ACTION_TYPE_ATTR = "Type";

    public static final String UFT_ACTION_KIND_VALUE = "16";

    public static final String UFT_ACTION_TYPE_VALUE = "1";

    public static UftTestDiscoveryResult doFullDiscovery(File root) {
        return doFullDiscovery(root, TestingToolType.UFT);
    }

    public static UftTestDiscoveryResult doFullDiscovery(File root, TestingToolType testingToolType) {
        UftTestDiscoveryResult result = new UftTestDiscoveryResult();
        scanFileSystemRecursively(root, root, result, testingToolType);
        result.setFullScan(true);
        result.setTestingToolType(testingToolType);
        return result;
    }

    private static void scanFileSystemRecursively(File root, File dirPath, UftTestDiscoveryResult discoveryResult, TestingToolType testingToolType) {
        if (dirPath.isDirectory() && SKIP_FOLDERS.contains(dirPath.getName())) {
            return;
        }

        File[] paths = dirPath.isDirectory() ? dirPath.listFiles() : new File[]{dirPath};

        //if it test folder - create new test, else drill down to subFolders
        UftTestType testType = paths != null ? isUftTestFolder(paths) : UftTestType.None;
        if (!testType.isNone()) {
            // if UFT mode or (MBT mode and test type GUI)- API tests are currently not supported in MBT mode
            if (!(TestingToolType.MBT.equals(testingToolType) && (UftTestType.API.equals(testType)))) {
                AutomatedTest test = createAutomatedTest(root, dirPath, testType, testingToolType);
                discoveryResult.getAllTests().add(test);
            }
        } else if (paths != null) {
            for (File path : paths) {
                if (path.isDirectory()) {
                    scanFileSystemRecursively(root, path, discoveryResult, testingToolType);
                } else if (isUftDataTableFile(path.getName())) {
                    ScmResourceFile dataTable = createDataTable(root, path);
                    discoveryResult.getAllScmResourceFiles().add(dataTable);
                }
            }
        }
    }

    public static ScmResourceFile createDataTable(File root, File path) {
        ScmResourceFile resourceFile = new ScmResourceFile();
        resourceFile.setName(path.getName());
        resourceFile.setRelativePath(getRelativePath(root, path));
        resourceFile.setOctaneStatus(OctaneStatus.NEW);

        return resourceFile;

    }

    public static boolean isUftDataTableFile(String path) {
        String loweredPath = path.toLowerCase();
        return loweredPath.endsWith(XLSXExtention) || loweredPath.endsWith(XLSExtention);
    }

    public static UftTestType isUftTestFolder(File[] paths) {
        for (File path : paths) {
            if (path.getName().endsWith(STFileExtention)) {
                return UftTestType.API;
            }
            if (path.getName().endsWith(QTPFileExtention)) {
                return UftTestType.GUI;
            }
        }

        return UftTestType.None;
    }

    public static boolean isTestMainFilePath(String path) {
        return !getUftTestType(path).isNone();
    }

    public static UftTestType getUftTestType(String testMainFilePath) {
        String lowerPath = testMainFilePath.toLowerCase();
        if (lowerPath.endsWith(QTPFileExtention)) {
            return UftTestType.GUI;
        }

        if (lowerPath.endsWith(STFileExtention) || lowerPath.endsWith(API_ACTIONS_FILE)) {
            return UftTestType.API;
        }

        return UftTestType.None;
    }

    public static File getTestFolderForTestMainFile(String path) {
        if (isTestMainFilePath(path)) {
            File file = new File(path);
            File parent = file.getParentFile();
            return parent;
        }
        return null;
    }

    public static AutomatedTest createAutomatedTest(File root, File dirPath, UftTestType testType, TestingToolType testingToolType) {
        AutomatedTest test = new AutomatedTest();
        test.setName(dirPath.getName());

        String relativePath = getRelativePath(root, dirPath);
        String packageName = relativePath.length() != dirPath.getName().length() ? relativePath.substring(0, relativePath.length() - dirPath.getName().length() - 1) : "";
        test.setPackage(packageName);
        test.setExecutable(true);
        test.setUftTestType(testType);

        Document testDocument = getDocument(dirPath, testType);

        String description = getTestDescription(testDocument, testType);
        description = convertToHtmlFormatIfRequired(description);
        test.setDescription(description);
        test.setOctaneStatus(OctaneStatus.NEW);

        // discover actions only for mbt testingToolType and gui tests
        if (TestingToolType.MBT.equals(testingToolType) && UftTestType.GUI.equals(testType)) {
            String actionPathPrefix = (SdkStringUtils.isEmpty(test.getPackage()) ? "" : test.getPackage() + "\\") +
                    test.getName() + ":";
            test.setActions(parseActionsAndParameters(testDocument, actionPathPrefix, test.getName(), dirPath));
        }

        return test;
    }

    public static String convertToHtmlFormatIfRequired(String description) {
        if (description == null || !description.contains("\n")) {
            return description;
        }
        //aaa\nbbb = >   <html><body><p>aaa</p><p>bbb</p></body></html>
        String[] lines = description.split("\n");
        StringBuilder sb = new StringBuilder(description.length() + lines.length * 10 + 30);
        sb.append("<html><body>");
        for (String line : lines) {
            sb.append("<p>");
            sb.append(line);
            sb.append("</p>");
            sb.append("\n");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String getRelativePath(File root, File path) {
        String testPath = path.getPath();
        String rootPath = root.getPath();
        String relativePath = testPath.replace(rootPath, "");
        relativePath = SdkStringUtils.strip(relativePath, SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER + SdkConstants.FileSystem.LINUX_PATH_SPLITTER);
        //we want all paths will be in windows style, because tests are run in windows, therefore we replace all linux splitters (/) by windows one (\)
        //http://stackoverflow.com/questions/23869613/how-to-replace-one-or-more-in-string-with-just
        relativePath = relativePath.replaceAll(SdkConstants.FileSystem.LINUX_PATH_SPLITTER, SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER + SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER);//str.replaceAll("/", "\\\\");
        return relativePath;
    }

    /**
     * Extract test description from UFT GUI test.
     * Note : UFT API test doesn't contain description
     *
     * @param testDocument document created from the test path
     * @param testType     GUI or API
     * @return test description
     */
    public static String getTestDescription(Document testDocument, UftTestType testType) {
        if (Objects.isNull(testDocument) || testType.isNone()) {
            return null;
        }

        String description;
        if (UftTestType.GUI.equals(testType)) {
            description = getTestDescriptionFromGuiTest(testDocument);
        } else {
            description = getTestDescriptionFromAPITest(testDocument);
        }
        if (description != null) {
            description = description.trim();
        }
        return description;
    }

    private static String getTestDescriptionFromAPITest(Document document) {

        //Actions.xml
        //<Actions>
        //<Action internalName="MainAction" userDefinedName="APITest1" description="radi end test description" />
        //</Actions>

        NodeList actions = document.getElementsByTagName("Action");
        for (int temp = 0; temp < actions.getLength(); temp++) {
            Node nNode = actions.item(temp);
            NamedNodeMap attributes = nNode.getAttributes();
            Node internalNameAttr = attributes.getNamedItem("internalName");
            if (internalNameAttr != null && "MainAction".equals(internalNameAttr.getNodeValue())) {
                return attributes.getNamedItem("description").getNodeValue();
            }
        }
        return null;
    }

    private static String getTestDescriptionFromGuiTest(Document document) {
        return document.getElementsByTagName("Description").item(0).getTextContent();
    }

    public static String extractXmlContentFromTspFile(InputStream stream) throws IOException {
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
                    int readBytes = poiFS.createDocumentInputStream("ComponentInfo").read(content);
                    if (readBytes < content.length) {
                        //  [YG] probably should handle this case and continue to read
                        logger.warn("expected to read " + content.length + " bytes, but read and stopped after " + readBytes);
                    }
                    String fromUnicodeLE = StringUtil.getFromUnicodeLE(content);
                    xmlData = fromUnicodeLE.substring(fromUnicodeLE.indexOf('<')).replaceAll("\u0000", "");
                }
            }
        }
        return xmlData;
    }

    public static Document getDocument(File dirPath, UftTestType testType) {
        if (Objects.isNull(dirPath) || !dirPath.exists()) {
            logger.error("test path is expected to be non null or exist");

            return null;
        }

        try {
            if (UftTestType.GUI.equals(testType)) {
                return getGuiTestDocument(dirPath);
            } else {
                return getApiTestDocument(dirPath);
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            logger.error("Failed to create document for path: {}, test type: {}", dirPath.getPath(), testType.name(), e);
            return null;
        }
    }

    private static Document getGuiTestDocument(File dirPath) throws IOException, ParserConfigurationException, SAXException {
        File tspTestFile = new File(dirPath, GUI_TEST_FILE);
        if (!tspTestFile.exists()) {
            return null;
        }

        InputStream is = new FileInputStream(tspTestFile);
        String xmlContent = extractXmlContentFromTspFile(is);

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        return documentBuilder.parse(new InputSource(new StringReader(xmlContent)));
    }

    private static Document getApiTestDocument(File dirPath) throws ParserConfigurationException, IOException, SAXException {
        File actionsFile = new File(dirPath, API_ACTIONS_FILE);
        if (!actionsFile.exists()) {
            return null;
        }

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        return dBuilder.parse(actionsFile);
    }

    private static List<UftTestAction> parseActionsAndParameters(Document document, String actionPathPrefix, String testName, File dirPath) {
        List<UftTestAction> actions = new ArrayList<>();

        if (Objects.isNull(document)) {
            logger.warn("received null gui test document, actions will not be parsed");
        } else {
            Map<String, UftTestAction> actionMap = parseActionComponents(document, testName);
            fillActionsLogicalName(document, actionMap, actionPathPrefix);
            actions.addAll(actionMap.values());
            try {
                readParameters(dirPath, actionMap);
            } catch (IOException | ParserConfigurationException | SAXException e) {
                logger.error("failed to parse action's parameters", e);
            }
        }

        return actions;
    }

    private static Map<String, UftTestAction> parseActionComponents(Document document, String testName) {
        Map<String, UftTestAction> actionMap = new HashMap<>();

        NodeList componentNodes = document.getElementsByTagName(UFT_COMPONENT_NODE_NAME);
        Node componentNode;
        for (int i = 0; i < componentNodes.getLength(); i++) {
            componentNode = componentNodes.item(i);
            String actionName = componentNode.getTextContent();
            if (!actionName.equalsIgnoreCase(ACTION_0)) { // filter action0
                UftTestAction action = new UftTestAction();
                action.setName(testName + ":" + actionName);
                actionMap.put(actionName, action);
            }
        }

        return actionMap;
    }

    private static void fillActionsLogicalName(Document document, Map<String, UftTestAction> actionMap, String actionPathPrefix) {
        NodeList dependencyNodes = document.getElementsByTagName(UFT_DEPENDENCY_NODE_NAME);
        Node dependencyNode;
        NamedNodeMap attributes;
        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            dependencyNode = dependencyNodes.item(i);
            attributes = dependencyNode.getAttributes();
            String type = attributes.getNamedItem(UFT_ACTION_TYPE_ATTR).getNodeValue();
            String kind = attributes.getNamedItem(UFT_ACTION_KIND_ATTR).getNodeValue();
            String logicalName = attributes.getNamedItem(UFT_ACTION_LOGICAL_ATTR).getNodeValue();

            if (type.equals(UFT_ACTION_TYPE_VALUE) && kind.equals(UFT_ACTION_KIND_VALUE) && SdkStringUtils.isNotEmpty(logicalName)) {
                String dependencyStr = dependencyNode.getTextContent();
                String actionName = dependencyStr.substring(0, dependencyStr.indexOf("\\"));
                if (!actionName.equalsIgnoreCase(ACTION_0)) { // action0 is not relevant
                    UftTestAction action = actionMap.get(actionName);
                    action.setLogicalName(logicalName);
                    setActionPath(action, actionPathPrefix);
                }
            }
        }
    }

    private static void setActionPath(UftTestAction action, String actionPathPrefix) {
        String actionName = SdkStringUtils.isEmpty(action.getLogicalName()) ? action.getName() : action.getLogicalName();
        action.setRepositoryPath(actionPathPrefix + actionName);
    }

    private static void readParameters(File testDirPath, Map<String, UftTestAction> actionMap) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        for (Map.Entry<String, UftTestAction> entry : actionMap.entrySet()) {
            String actionName = entry.getKey();
            File actionFolder = new File(testDirPath, actionName);
            if (actionFolder.exists()) {
                File resourceMtrFile = new File(actionFolder, RESOURCE_MTR_FILE);
                if (resourceMtrFile.exists()) {
                    entry.getValue().setParameters(parseParameterFile(resourceMtrFile, documentBuilder));
                }
            } else {
                entry.getValue().setParameters(Collections.emptyList());
                logger.warn("folder for action {} does not exist", actionName);
            }
        }
    }

    private static List<UftTestParameter> parseParameterFile(File resourceMtrFile, DocumentBuilder documentBuilder) throws IOException, SAXException {
        List<UftTestParameter> parameters = new ArrayList<>();
        InputStream is = new FileInputStream(resourceMtrFile);
        String xmlContent = extractXmlContentFromTspFile(is);
        Document document = documentBuilder.parse(new InputSource(new StringReader(xmlContent)));
        NodeList argumentsCollectionElement = document.getElementsByTagName(UFT_PARAM_ARGUMENTS_COLLECTION_NODE_NAME);
        if (argumentsCollectionElement.getLength() > 0) {
            Node argumentsCollectionItem = argumentsCollectionElement.item(0);
            NodeList childArgumentElements = argumentsCollectionItem.getChildNodes();
            for (int i = 0; i < childArgumentElements.getLength(); i++) {
                Element argumentElement = (Element) childArgumentElements.item(i);
                UftTestParameter parameter = new UftTestParameter();
                parameter.setName(argumentElement.getElementsByTagName(UFT_PARAM_ARG_NAME_NODE_NAME).item(0).getTextContent());
                parameter.setDirection(UftParameterDirection.get(Integer.parseInt(argumentElement.getElementsByTagName("ArgDirection").item(0).getTextContent())));
                Node defaultValueNode = argumentElement.getElementsByTagName(UFT_PARAM_ARG_DEFAULT_VALUE_NODE_NAME).item(0);
                if (null != defaultValueNode) {
                    parameter.setDefaultValue(defaultValueNode.getTextContent());
                }
                parameters.add(parameter);
            }
        }

        return parameters;
    }

}
