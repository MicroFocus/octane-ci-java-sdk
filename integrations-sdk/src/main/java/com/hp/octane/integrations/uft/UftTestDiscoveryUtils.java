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

import com.hp.octane.integrations.uft.items.*;
import com.hp.octane.integrations.utils.SdkConstants;
import com.hp.octane.integrations.utils.SdkStringUtils;
import org.apache.poi.poifs.filesystem.*;
import org.apache.poi.util.StringUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UftTestDiscoveryUtils {

	private static final String STFileExtention = ".st";//api test
	private static final String QTPFileExtention = ".tsp";//gui test
	private static final String XLSXExtention = ".xlsx";//excel file
	private static final String XLSExtention = ".xls";//excel file

	private static final Set<String> SKIP_FOLDERS = Stream.of("_discovery_results").collect(Collectors.toSet());

	public static UftTestDiscoveryResult doFullDiscovery(File root) {
		UftTestDiscoveryResult result = new UftTestDiscoveryResult();
		scanFileSystemRecursively(root, root, result);
		result.setFullScan(true);
		return result;
	}

	private static void scanFileSystemRecursively(File root, File dirPath, UftTestDiscoveryResult discoveryResult) {
		if (dirPath.isDirectory() && SKIP_FOLDERS.contains(dirPath.getName())) {
			return;
		}

		File[] paths = dirPath.isDirectory() ? dirPath.listFiles() : new File[]{dirPath};

		//if it test folder - create new test, else drill down to subFolders
		UftTestType testType = paths != null ? isUftTestFolder(paths) : UftTestType.None;
		if (!testType.isNone()) {
			AutomatedTest test = createAutomatedTest(root, dirPath, testType);
			discoveryResult.getAllTests().add(test);
		} else if (paths != null) {
			for (File path : paths) {
				if (path.isDirectory()) {
					scanFileSystemRecursively(root, path, discoveryResult);
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
		String lowerPath = path.toLowerCase();
		if (lowerPath.endsWith(STFileExtention)) {
			return true;
		} else if (lowerPath.endsWith(QTPFileExtention)) {
			return true;
		}

		return false;
	}

	public static UftTestType getUftTestType(String testMainFilePath) {
		String lowerPath = testMainFilePath.toLowerCase();
		if (lowerPath.endsWith(STFileExtention)) {
			return UftTestType.API;
		} else if (lowerPath.endsWith(QTPFileExtention)) {
			return UftTestType.GUI;
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

	public static AutomatedTest createAutomatedTest(File root, File dirPath, UftTestType testType) {
		AutomatedTest test = new AutomatedTest();
		test.setName(dirPath.getName());

		String relativePath = getRelativePath(root, dirPath);
		String packageName = relativePath.length() != dirPath.getName().length() ? relativePath.substring(0, relativePath.length() - dirPath.getName().length() - 1) : "";
		test.setPackage(packageName);
		test.setExecutable(true);

		if (testType != null && !testType.isNone()) {
			test.setUftTestType(testType);
		}

		String description = getTestDescription(dirPath);
		test.setDescription(description);
		test.setOctaneStatus(OctaneStatus.NEW);


		return test;
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
	 * @param dirPath path of UFT test
	 * @return test description
	 */
	private static String getTestDescription(File dirPath) {

		try {
			if (!dirPath.exists()) {
				return null;
			}

			File tspTestFile = new File(dirPath, "Test.tsp");
			if (!tspTestFile.exists()) {
				return null;
			}


			InputStream is = new FileInputStream(tspTestFile);
			String xmlContent = extractXmlContentFromTspFile(is);


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

	private static String extractXmlContentFromTspFile(InputStream stream) throws IOException {
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
