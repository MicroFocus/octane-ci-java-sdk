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
package com.hp.octane.integrations.services.vulnerabilities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.exceptions.OctaneSDKGeneralException;
import com.hp.octane.integrations.exceptions.PermanentException;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssuesFileSerializer {
	private String targetDir;
	private List<OctaneIssue> octaneIssues;

	public IssuesFileSerializer(String targetDir, List<OctaneIssue> issues) {
		this.targetDir = targetDir;
		this.octaneIssues = issues;
	}

	public InputStream doSerializeAndCache() {
		try {
			validateFolderExists();
			Map<String, List<OctaneIssue>> dataFormat = new HashMap<>();
			dataFormat.put("data", octaneIssues);
			ObjectMapper mapper = new ObjectMapper();
			mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			mapper.writeValue(baos, dataFormat);
			InputStream is = new ByteArrayInputStream(baos.toByteArray());

			//send to cache
			if (targetDir != null) {
				String vulnerabilitiesScanFilePath = targetDir + File.separator + SSCHandler.SCAN_RESULT_FILE;
				PrintWriter fw = new PrintWriter(vulnerabilitiesScanFilePath, "UTF-8");
				mapper.writeValue(fw, dataFormat);
				fw.flush();
				fw.close();
			}
			return is;
		} catch (Exception e) {
			throw new PermanentException(e);
		}
	}

	private void validateFolderExists() {
		File file = new File(this.targetDir);
		if (!file.exists()) {
			if (!file.mkdirs()) {
				throw new OctaneSDKGeneralException("target directory was missing and failed to create one");
			}
		}
	}
}
