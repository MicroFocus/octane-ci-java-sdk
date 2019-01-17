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

package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.exceptions.OctaneSDKGeneralException;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.vulnerabilities.*;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.IssueDetails;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Issues;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class SSCServiceImpl implements SSCService{

    private static final Logger logger = LogManager.getLogger(SSCServiceImpl.class);
    protected final OctaneSDK.SDKServicesConfigurer configurer;
    protected final RestService restService;




    public SSCServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
        if (configurer == null) {
            throw new IllegalArgumentException("invalid configurer");
        }
        if (restService == null) {
            throw new IllegalArgumentException("rest service MUST NOT be null");
        }

        this.configurer = configurer;
        this.restService = restService;
    }

    @Override
    public OctaneSDK.SDKServicesConfigurer getConfigurer() {
        return configurer;
    }

    @Override
    public RestService getRestService() {
        return restService;
    }

    @Override
    public InputStream getVulnerabilitiesScanResultStream(VulnerabilitiesQueueItem queueItem) {

        try {
            String targetDir = getTargetDir(queueItem);
            InputStream cachedScanResult = getCachedScanResult(targetDir);
            if (cachedScanResult != null) {
                return cachedScanResult;
            }
            List<OctaneIssue> octaneIssues = getNonCacheVulnerabilitiesScanResultStream(queueItem);
            if(octaneIssues==null){
                return null;
            }
            cacheIssues(targetDir,octaneIssues);
            return  IssuesFileSerializer.serializeIssues(octaneIssues);
        }
        catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean vulnerabilitiesQueueItemCleanUp(VulnerabilitiesQueueItem vulnerabilitiesQueueItem){
        String runRootDir = getTargetDir(vulnerabilitiesQueueItem);
        if (runRootDir == null) {
            return false;
        }
        File directoryToBeDeleted = new File(runRootDir);
        return deleteDirectory(directoryToBeDeleted);
    }


    private List<OctaneIssue> getNonCacheVulnerabilitiesScanResultStream(VulnerabilitiesQueueItem queueItem
                                                                         ) throws IOException {
        SSCProjectConfiguration sscProjectConfiguration = configurer.pluginServices.getSSCProjectConfiguration(queueItem.getJobId(), queueItem.getBuildId());
        if (sscProjectConfiguration == null || !sscProjectConfiguration.isValid()) {
            logger.debug("SSC project configurations is missing or not valid, skipping processing for " + queueItem.getJobId() + " #" + queueItem.getBuildId());
            return null;
        }


        SSCHandler sscHandler = new SSCHandler(
                queueItem,
                sscProjectConfiguration,
                this.restService.obtainSSCRestClient());



        List<Issues.Issue> issuesFromSecurityTool = getIssuesFromSSC(sscHandler,queueItem);
        if(issuesFromSecurityTool==null){
            return null;
        }

        List<String> octaneExistsIssuesIdsList = getRemoteIdsOfExistIssuesFromOctane(queueItem, sscProjectConfiguration.getRemoteTag());

        List<Issues.Issue> issuesRequiredExtendedData = issuesFromSecurityTool.stream().filter(
                t -> {
                    boolean isNew = t.scanStatus.equalsIgnoreCase("NEW");
                    boolean isMissing = false;
                    if(queueItem.getBaselineDate()!=null){
                        isMissing = !octaneExistsIssuesIdsList.contains(t.issueInstanceId);
                    }
                    return isNew||isMissing;
                }).collect(
                Collectors.toList());

        Map<Integer, IssueDetails> issuesWithExtendedData = sscHandler.getIssuesExtendedData(issuesRequiredExtendedData);

        PackSSCIssuesToSendToOctane packSSCIssuesToSendToOctane = new PackSSCIssuesToSendToOctane();
        return packSSCIssuesToSendToOctane.packAllIssues(issuesFromSecurityTool,
                octaneExistsIssuesIdsList,
                sscProjectConfiguration.getRemoteTag(),
                issuesWithExtendedData);
    }

    //return issues from security tool (or null if scan not completed)
    private List<Issues.Issue> getIssuesFromSSC(SSCHandler sscHandler, VulnerabilitiesQueueItem vulnerabilitiesQueueItem) {
        Optional<Issues> allIssues = sscHandler.getIssuesIfScanCompleted();
        if (!allIssues.isPresent()) {
            return null;
        }
        List<Issues.Issue> filterIssuesByBaseLine = allIssues.get().getData();
        //in case we have the baselineDate - we should filter by it to have more optimal payload
        if(vulnerabilitiesQueueItem.getBaselineDate()!=null) {
            filterIssuesByBaseLine = allIssues.get().getData().stream().filter(issue -> {
                Date foundDate = DateUtils.getDateFromUTCString(issue.foundDate, DateUtils.sscFormat);
                return foundDate.compareTo(vulnerabilitiesQueueItem.getBaselineDate()) >= 0;
            }).collect(Collectors.toList());
        }
        return filterIssuesByBaseLine;
    }

    private String getTargetDir(VulnerabilitiesQueueItem vulnerabilitiesQueueItem) {
        File allowedOctaneStorage = configurer.pluginServices.getAllowedOctaneStorage();
        if (allowedOctaneStorage == null) {
            logger.info("hosting plugin does not provide storage, vulnerabilities won't be cached");
            return null;
        }
        return allowedOctaneStorage.getPath() + File.separator + vulnerabilitiesQueueItem.getJobId() + File.separator + vulnerabilitiesQueueItem.getBuildId();
    }



    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private InputStream getCachedScanResult(String runRootDir) {
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

    private static void cacheIssues(String targetDir, List<OctaneIssue> octaneIssues) {
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

    private static void validateFolderExists(String targetDir) {
        File file = new File(targetDir);
        if (!file.exists() && !file.mkdirs()) {
            throw new OctaneSDKGeneralException("target directory was missing and failed to create one");
        }
    }


}
