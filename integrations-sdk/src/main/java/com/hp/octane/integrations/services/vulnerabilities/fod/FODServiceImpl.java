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
package com.hp.octane.integrations.services.vulnerabilities.fod;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.vulnerabilities.ExistingIssuesInOctane;
import com.hp.octane.integrations.services.vulnerabilities.PackIssuesToOctaneUtils;
import com.hp.octane.integrations.services.vulnerabilities.VulnerabilitiesQueueItem;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.pojos.Scan;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.pojos.Vulnerability;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.pojos.VulnerabilityAllData;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.services.FODReleaseService;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.services.FODVulnerabilityService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.hp.octane.integrations.services.vulnerabilities.IssuesFileSerializer.*;
import static com.hp.octane.integrations.services.vulnerabilities.fod.SecurityIssueValuesHelper.sameDay;

public class FODServiceImpl implements FODService {

    private static final Logger logger = LogManager.getLogger(FODServiceImpl.class);
    protected final OctaneSDK.SDKServicesConfigurer configurer;
    protected final RestService restService;

    public FODServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
        if (restService == null) {
            throw new IllegalArgumentException("rest service MUST NOT be null");
        }
        if (configurer == null) {
            throw new IllegalArgumentException("configurer service MUST NOT be null");
        }
        this.configurer = configurer;
        this.restService = restService;
    }

    @Override
    public InputStream getVulnerabilitiesScanResultStream(VulnerabilitiesQueueItem queueItem) throws IOException {

        logger.debug("Entered getVulnerabilitiesScanResultStream");
        String targetDir = getTargetDir(configurer.pluginServices.getAllowedOctaneStorage(),
                queueItem.getJobId(), queueItem.getBuildId());
        logger.debug("getVulnerabilitiesScanResultStream target Dir:" + targetDir);
        InputStream cachedScanResult = getCachedScanResult(targetDir);
        if (cachedScanResult != null) {
            logger.warn("results "+ queueItem.toString()+ "are cached!");
            return cachedScanResult;
        }
        enrichItemWithFODParams(queueItem);
        PplnRunStatus pplnRunStatus = fodScanIsStillInProgress(queueItem);
        if (pplnRunStatus.continuePolling) {
            return null;
        } else if (pplnRunStatus.tryGetIssues) {
            List<OctaneIssue> octaneIssues =  fetchIssues(queueItem, getRelease(queueItem).toString());
            cacheIssues(targetDir,octaneIssues);
            return  serializeIssues(octaneIssues);

        } else {
            throw new PermanentException(queueItem.getJobId() +"#"+ queueItem.getBuildId() +
                    " , Polling is stopped");
        }
    }

    @Override
    public boolean vulnerabilitiesQueueItemCleanUp(VulnerabilitiesQueueItem queueItem) {
        return false;
    }

    @Override
    public RestService getRestService() {
        return this.restService;
    }

    @Override
    public OctaneSDK.SDKServicesConfigurer getConfigurer() {
        return this.configurer;
    }

    private PplnRunStatus fodScanIsStillInProgress(VulnerabilitiesQueueItem queueItem) {

        logger.debug("Check if scan is in progress." + queueItem.getJobId() +"#" +queueItem.getJobId());

        Long pplRunStartTime = queueItem.getStartTime();

        //It is done , but it's scan still active.
        if (getScan(queueItem) == null) {
            logger.debug("need to retrieve the scan Id");
            List<Scan> scans = FODReleaseService.getScansLastInFirstFetched(getRelease(queueItem), pplRunStartTime);
            setScanIdForItem(queueItem, scans);
            if (getScan(queueItem) == null) {
                incFailedTries(queueItem);
                logger.warn("scan Id was not found yet");
            }
        } else {
            logger.debug("scanId is already retrieved from previous polling:" + getScan(queueItem));
        }
        if (getScan(queueItem) != null) {
            Long release = getRelease(queueItem);
            Long scan = getScan(queueItem);
            if (scanIsCompleted(release, scan)) {
                return new PplnRunStatus(false, true);
            }
        }
        if (getFailedTries(queueItem) > 10) {
            logger.error(
                    "scan Id was not found, validate that the release in the pipeline configuration is the same as the release in the Jenkins job.");
            return new PplnRunStatus(false, false);
        }
        return new PplnRunStatus(true, false);
    }

    private void incFailedTries(VulnerabilitiesQueueItem queueItem) {
        String failedTriesToGetScanStr = queueItem.getAdditionalProperties().
                get("failedTriesToGetScan");
        Integer failedTriesToGetScan = 1;
        if(failedTriesToGetScanStr != null){
            failedTriesToGetScan = Integer.parseInt(failedTriesToGetScanStr) + 1;
        }
        queueItem.getAdditionalProperties().put("failedTriesToGetScan", failedTriesToGetScan.toString());
    }
    private int getFailedTries(VulnerabilitiesQueueItem queueItem) {
        String failedTriesToGetScanStr = queueItem.getAdditionalProperties().
                get("failedTriesToGetScan");
        return failedTriesToGetScanStr == null ?  0 : Integer.parseInt(failedTriesToGetScanStr);
    }

    private void enrichItemWithFODParams(VulnerabilitiesQueueItem queueItem) {
        if(queueItem.getAdditionalProperties() == null){
            queueItem.setAdditionalProperties(new HashMap<>());
        }
        String releaseId = queueItem.getAdditionalProperties().get("releaseId");
        if (releaseId == null) {
            releaseId = this.configurer.pluginServices.getFodRelease(queueItem.getJobId(), queueItem.getBuildId()).toString();
            queueItem.getAdditionalProperties().put("releaseId", releaseId);
        }
        logger.warn("FOD ReleaseId:" + releaseId);
    }

    Long getRelease(VulnerabilitiesQueueItem item){
        return Long.valueOf(item.getAdditionalProperties().get("releaseId"));

    }
    Long getScan(VulnerabilitiesQueueItem item){
        return !item.getAdditionalProperties().containsKey("scanId") ? null :
                Long.valueOf(item.getAdditionalProperties().get("scanId"));
    }
    private List<OctaneIssue> fetchIssues(VulnerabilitiesQueueItem queueItem, String remoteTag) throws IOException {

        logger.warn("Security scan is done, time to get issues from.");
        List<Vulnerability> allVulnerabilities =
                FODVulnerabilityService.getAllVulnerabilities(getRelease(queueItem));
        List<Vulnerability> nonClosedIssues = filterOutBeforeBaselineIssues(queueItem.getBaselineDate(),allVulnerabilities);
        ExistingIssuesInOctane existingIssuesInOctane = new ExistingIssuesInOctane(this.restService.obtainOctaneRestClient(),
                this.configurer.octaneConfiguration);
        List<String> existingIssuesInOc = existingIssuesInOctane.getRemoteIdsOpenVulnsFromOctane(queueItem.getJobId(),
                queueItem.getBuildId(), remoteTag);


        PackIssuesToOctaneUtils.SortedIssues<Vulnerability> sortedIssues =
                PackIssuesToOctaneUtils.packToOctaneIssues(nonClosedIssues, existingIssuesInOc,true);

        SecurityIssueValuesHelper securityIssueValuesHelper = new SecurityIssueValuesHelper();
        securityIssueValuesHelper.init();
        Map<String,VulnerabilityAllData> idToAllData = new HashMap<>();
        sortedIssues.issuesRequiredExtendedData.stream()
                .forEach(t -> idToAllData.put(t.id,
                        FODVulnerabilityService.getSingleVulnAlldata(getRelease(queueItem),t.vulnId)));

        List<OctaneIssue> octaneIssuesToUpdate =
                securityIssueValuesHelper.createOctaneIssuesFromVulns(sortedIssues.issuesToUpdate, remoteTag, idToAllData,
                        queueItem.getBaselineDate());

        List<OctaneIssue> total = new ArrayList<>();
        total.addAll(octaneIssuesToUpdate);
        logger.warn("ToUpdate:" + octaneIssuesToUpdate.toString());
        total.addAll(sortedIssues.issuesToClose);
        logger.warn("ToClose:" + sortedIssues.issuesToClose);
        return total;
    }

    private List<Vulnerability> filterOutBeforeBaselineIssues(Date baseline,
                                                              List<Vulnerability> allVulnerabilities) {

        return allVulnerabilities.stream().filter(
                t -> {
                    Date date = SecurityIssueValuesHelper.dateOfDateString(t.introducedDate);
                    return date.after(baseline) ||
                    sameDay(date, baseline);
                })
                .collect(Collectors.toList());

    }

    private boolean scanIsCompleted(Long releaseId, Long scanId) {
        try {
            Scan completeScan = FODReleaseService.getCompleteScan(releaseId, scanId);
            if (completeScan == null) {
                return false;
            }

            logger.debug("scan:" + scanId + " is:" + completeScan.status);

            if (completeScan.status == null) {
                return false;
            }
            //Scan that has not started, and not in progress is completed.
            return (!Scan.IN_PROGRESS.equals(completeScan.status) &&
                    !Scan.NOT_STARTED.equals(completeScan.status));
        } catch (Exception e) {
            return false;
        }
    }
    private void setScanIdForItem(VulnerabilitiesQueueItem queueItem, List<Scan> scans) {
        Long relevantScanId = getRelevantScan(scans, queueItem);
        logger.debug("scan Id is retrieved:" + relevantScanId);
        if(relevantScanId !=  null) {
            queueItem.getAdditionalProperties().put("scanId", relevantScanId.toString());
        }

    }
    private Long getRelevantScan(List<Scan> scans, VulnerabilitiesQueueItem queueItem) {

        Scan scanByNotes = getScanByNotes(scans, queueItem);
        if(scanByNotes != null){
            return scanByNotes.scanId;
        }
        for (Scan scan : scans) {
            if (Scan.IN_PROGRESS.equals(scan.status)) {
                return scan.scanId;
            }
        }
        return null;
    }

    private Scan getScanByNotes(List<Scan> scans,
                                VulnerabilitiesQueueItem queueItem) {
        //   "notes": "[80] #80 - Assessment submitted from Jenkins FoD Plugin".
        for(Scan scan : scans) {
            if (scan.notes != null &&
                    scan.notes.contains("[" + queueItem.getBuildId() + "]")) {
                return scan;
            }
        }
        return null;
    }
}
