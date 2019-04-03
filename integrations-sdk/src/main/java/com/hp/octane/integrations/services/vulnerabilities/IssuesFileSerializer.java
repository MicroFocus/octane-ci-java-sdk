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
import com.hp.octane.integrations.services.vulnerabilities.ssc.SSCHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssuesFileSerializer {
    private static final Logger logger = LogManager.getLogger(IssuesFileSerializer.class);

    public static InputStream serializeIssues(List<OctaneIssue> octaneIssues) {
        try {
            Map<String, List<OctaneIssue>> dataFormat = new HashMap<>();
            dataFormat.put("data", octaneIssues);
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mapper.writeValue(baos, dataFormat);
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            return is;

        } catch (Exception e) {
            throw new PermanentException(e);
        }
    }

    public static String getTargetDir(File allowedOctaneStorage, String jobId, String buildId) {

        if (allowedOctaneStorage == null) {
            logger.info("hosting plugin does not provide storage, vulnerabilities won't be cached");
            return null;
        }
        return allowedOctaneStorage.getPath() + File.separator + jobId + File.separator +
                buildId;
    }

    public static InputStream getCachedScanResult(String runRootDir) {
        if (runRootDir == null) {
            return null;
        }
        InputStream result = null;
        String vulnerabilitiesScanFilePath = runRootDir + File.separator + "securityScan.json";
        File vulnerabilitiesScanFile = new File(vulnerabilitiesScanFilePath);
        if (!vulnerabilitiesScanFile.exists()) {
            return null;
        }
        try {
            result = new FileInputStream(vulnerabilitiesScanFilePath);
        } catch (IOException ioe) {
            logger.error("failed to obtain  vulnerabilities Scan File in " + runRootDir);
        }
        return result;
    }

    public static void cacheIssues(String targetDir, List<OctaneIssue> octaneIssues) {
        try {
            if (targetDir != null) {
                validateFolderExists(targetDir);
                Map<String, List<OctaneIssue>> dataFormat = new HashMap<>();
                dataFormat.put("data", octaneIssues);
                ObjectMapper mapper = new ObjectMapper();
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                //send to cache

                String vulnerabilitiesScanFilePath = targetDir + File.separator + SSCHandler.SCAN_RESULT_FILE;
                PrintWriter fw = new PrintWriter(vulnerabilitiesScanFilePath, "UTF-8");
                mapper.writeValue(fw, dataFormat);
                fw.flush();
                fw.close();
            }
        } catch (Exception e) {
            throw new PermanentException(e);
        }
    }

    public static void validateFolderExists(String targetDir) {
        File file = new File(targetDir);
        if (!file.exists() && !file.mkdirs()) {
            throw new OctaneSDKGeneralException("target directory was missing and failed to create one");
        }
    }


    public static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

}
