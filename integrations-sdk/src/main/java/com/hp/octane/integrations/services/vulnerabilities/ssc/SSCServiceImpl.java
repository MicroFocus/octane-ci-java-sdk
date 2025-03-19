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
package com.hp.octane.integrations.services.vulnerabilities.ssc;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.vulnerabilities.DateUtils;
import com.hp.octane.integrations.services.vulnerabilities.IssuesFileSerializer;
import com.hp.octane.integrations.services.vulnerabilities.VulnerabilitiesQueueItem;
import com.hp.octane.integrations.services.vulnerabilities.ssc.dto.Issues;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.hp.octane.integrations.services.vulnerabilities.IssuesFileSerializer.*;

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
            String targetDir = getTargetDir(getConfigurer().pluginServices.getAllowedOctaneStorage(),
                    queueItem.getJobId(),
                    queueItem.getBuildId());
            logger.debug(configurer.octaneConfiguration.getLocationForLog() + "targetDir:" + targetDir);
            InputStream cachedScanResult = getCachedScanResult(targetDir);
            if (cachedScanResult != null) {
                logger.warn(configurer.octaneConfiguration.getLocationForLog() + "Results are cached.");
                return cachedScanResult;
            }
            logger.debug("before getNonCacheVulnerabilitiesScanResultStream queueItem:" + queueItem.getJobId());
            List<OctaneIssue> octaneIssues = getNonCacheVulnerabilitiesScanResultStream(queueItem);
            logger.debug(configurer.octaneConfiguration.getLocationForLog() + "Done retrieving non-cached.");
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
        String runRootDir = getTargetDir(configurer.pluginServices.getAllowedOctaneStorage(),
                vulnerabilitiesQueueItem.getJobId(),
                vulnerabilitiesQueueItem.getBuildId());
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
            logger.error(configurer.octaneConfiguration.getLocationForLog() + "cannot retrieve SSC Project CFG.");
            logger.debug(configurer.octaneConfiguration.getLocationForLog() + "SSC project configurations is missing or not valid, skipping processing for " + queueItem.getJobId() + " #" + queueItem.getBuildId());
             return null;
        }

        SSCHandler sscHandler = new SSCHandler(
                queueItem,
                sscProjectConfiguration,
                this.restService.obtainSSCRestClient());

        logger.debug(configurer.octaneConfiguration.getLocationForLog() + "retrieve issues from SSC");
        List<Issues.Issue> issuesFromSecurityTool = getIssuesFromSSC(sscHandler,queueItem);
        if(issuesFromSecurityTool==null){
            logger.debug("issuesFromSecurityTool is null" );

            return null;
        }
        logger.debug(configurer.octaneConfiguration.getLocationForLog() + "retrieve octane remote ids");

        List<String> octaneExistsIssuesIdsList = getRemoteIdsOfExistIssuesFromOctane(queueItem, sscProjectConfiguration.getRemoteTag());
        logger.debug(configurer.octaneConfiguration.getLocationForLog() + "done retrieveing octane remote ids");

        PackSSCIssuesToSendToOctane packSSCIssuesToSendToOctane = new PackSSCIssuesToSendToOctane();
        if( queueItem.getBaselineDate() != null){
            logger.debug(" queueItem.getBaselineDate() + " + String.valueOf(queueItem.getBaselineDate()));
        } else {
            logger.debug(" queueItem.getBaselineDate() is null ");
        }
        packSSCIssuesToSendToOctane.setConsiderMissing(queueItem.getBaselineDate() != null);
        if ( !octaneExistsIssuesIdsList.isEmpty()){
            logger.debug(" octaneExistsIssuesIdsList" + octaneExistsIssuesIdsList);
        }
        packSSCIssuesToSendToOctane.setOctaneIssues(octaneExistsIssuesIdsList);
        packSSCIssuesToSendToOctane.setRemoteTag(sscProjectConfiguration.getRemoteTag());
        packSSCIssuesToSendToOctane.setSscHandler(sscHandler);
        packSSCIssuesToSendToOctane.setSscIssues(issuesFromSecurityTool);
        logger.debug(" before packSSCIssuesToSendToOctane.packToOctaneIssues() ");
        return packSSCIssuesToSendToOctane.packToOctaneIssues();
    }

    //return issues from security tool (or null if scan not completed)
    private List<Issues.Issue> getIssuesFromSSC(SSCHandler sscHandler, VulnerabilitiesQueueItem vulnerabilitiesQueueItem) {

        Optional<Issues> allIssues = sscHandler.getIssuesIfScanCompleted();
        if (!allIssues.isPresent()) {
            logger.debug( vulnerabilitiesQueueItem.toString() + " not completed yet");
            return null;
        }
        logger.debug( vulnerabilitiesQueueItem.toString() + " completed SSC scan.");
        List<Issues.Issue> filterIssuesByBaseLine = allIssues.get().getData();
        //in case we have the baselineDate - we should filter by it to have more optimal payload
        if(vulnerabilitiesQueueItem.getBaselineDate()!=null) {
            filterIssuesByBaseLine = allIssues.get().getData().stream().filter(issue -> {
                Date foundDate = DateUtils.getDateFromUTCString(issue.foundDate, DateUtils.sscFormat);
                return foundDate.compareTo(vulnerabilitiesQueueItem.getBaselineDate()) >= 0;
            }).collect(Collectors.toList());
        }
        logger.debug(configurer.octaneConfiguration.getLocationForLog() + "filterIssuesByBaseLine.size():" + filterIssuesByBaseLine.size());
        return filterIssuesByBaseLine;
    }


}
