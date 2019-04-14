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
package com.hp.octane.integrations.services.vulnerabilities.fod.dto.mock;



import com.hp.octane.integrations.services.vulnerabilities.fod.dto.FODEntityCollection;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.FODSource;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.pojos.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Predicate;

/**
 * Created by hijaziy on 12/24/2017.
 */
public class FodMockSource implements FODSource {

    int scansRequestCounter =0;
    int requestsUntilCompleted = 2;

    @Override
    public <T extends FODEntityCollection> T getAllFODEntities(String rawURL, Class<T> targetClass, Predicate<T> whenToStopFetch) {
        try {
            if (targetClass == Scan.Scans.class) {
                return getScans(targetClass);
            }else if(targetClass == Vulnerability.Vulnerabilities.class){
                return getVulnerabilities(targetClass);
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }




    private <T extends FODEntityCollection> T getVulnerabilities(Class<T> targetClass) throws InstantiationException, IllegalAccessException {
        T retVulns = targetClass.newInstance();
        Vulnerability vuln1 = new Vulnerability();
        vuln1.status = "New";
        vuln1.lineNumber = 10;
        vuln1.primaryLocationFull = "Location1";
        vuln1.severity = 3;
        vuln1.category = "CatA";
        vuln1.kingdom = "KingdomA";
        vuln1.subtype = "subA";
        vuln1.packageValue = "PackA";
        vuln1.vulnId =  "7d86320e-3a9d-4313-8e71-3e4e8cfcec30";
        Date dt = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        c.add(Calendar.DATE, 1);
        dt = c.getTime();
        vuln1.introducedDate =  new SimpleDateFormat("yyyy-MM-dd").format(dt);
        vuln1.reviewed = true;
        Vulnerability vuln2 = new Vulnerability();
        vuln2.status = "New";
        vuln2.lineNumber = 100;
        vuln2.primaryLocationFull = "Location2";
        vuln2.severity = 4;
        vuln2.category = "CatB";
        vuln2.kingdom = "KingdomB";
        vuln2.subtype = "subB";
        vuln2.packageValue = "PackB";
        vuln2.vulnId =  "7d86320e-3a9d-4313-8e71-3e4e8cfcec33";
        vuln2.introducedDate = new SimpleDateFormat("YYYY-MM-dd").format(dt);

        retVulns.items.add(vuln1);
        retVulns.items.add(vuln2);

        return retVulns;
    }

    private <T extends FODEntityCollection> T getScans(Class<T> targetClass) throws InstantiationException, IllegalAccessException {
        Scan scan1 = new Scan();
        scan1.status = "Completed";
        scan1.scanId = 1L;


        Scan scan2 = new Scan();
        scan2.status = "In_Progress";
        scan2.scanId = 2L;

        T retScans = targetClass.newInstance();
        retScans.items.add(scan1);
        retScans.items.add(scan2);
        return retScans;
    }


    @Override
    public <T> T getSpeceficFODEntity(String rawURL, Class<T> targetClass) {

        try {
            if (targetClass == Scan.class && rawURL.endsWith("/2")) {

                T retObj = null;
                Scan retScan;

                retObj = targetClass.newInstance();
                retScan = (Scan) retObj;

                retScan.scanId = 2L;
                scansRequestCounter++;
                if (scansRequestCounter < requestsUntilCompleted) {
                    retScan.status = "In_Progress";
                } else {
                    retScan.status = "Completed";
                    scansRequestCounter = 0;
                }
                return retObj;
            }
//            if (targetClass == VulnerabilityAllData.class) {
//                T retObj = null;
//                VulnerabilityAllData retAllData;
//                retObj = targetClass.newInstance();
//                retAllData = (VulnerabilityAllData) retObj;
//                retAllData.details = new VulnerabilityAllData.VulnDetails();
//                retAllData.details.summary = "Issue Summary";
//                retAllData.details.explanation = "Issue Explanation";
//                retAllData.recommendations = new VulnerabilityAllData.VulnRecommendation();
//                retAllData.recommendations.recommendations = "Issue recommendations";
//                retAllData.recommendations.tips = "Issue tips";
//                return retObj;
//            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getEntitiesURL() {
        return null;
    }
}
