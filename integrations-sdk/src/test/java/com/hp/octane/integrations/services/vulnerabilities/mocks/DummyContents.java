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
package com.hp.octane.integrations.services.vulnerabilities.mocks;

public class DummyContents {
    public static final String issuesPart1 = """
            {
              "data": [
                {
                  "bugURL": null,
                  "hidden": false,
                  "issueName": "Issue 1",
                  "folderGuid": "bb824e8d-b401-40be-13bd-5d156696a685",
                  "lastScanId": 155,
                  "engineType": "SCA",
                  "issueStatus": "Unreviewed",
                  "friority": "Low",
                  "analyzer": "Configuration",
                  "primaryLocation": "pom.xml",
                  "reviewed": null,
                  "id": 3708,
                  "suppressed": false,
                  "hasAttachments": false,
                  "engineCategory": "STATIC",
                  "projectVersionName": null,
                  "removedDate": null,
                  "severity": 2.0,
                  "_href": "http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/116/issues/3708",
                  "displayEngineType": "SCA",
                  "foundDate": "2018-10-09T07:43:16.000+0000",
                  "confidence": 5.0,
                  "impact": 2.0,
                  "primaryRuleGuid": "FF57412F-DD28-44DE-8F4F-0AD39620768C",
                  "projectVersionId": 116,
                  "scanStatus": "UPDATED",
                  "audited": false,
                  "kingdom": "Environment",
                  "folderId": 215,
                  "revision": 0,
                  "likelihood": 0.8,
                  "removed": false,
                  "issueInstanceId": "87E3EC5CC8154C006783CC461A6DDEEB",
                  "hasCorrelatedIssues": false,
                  "primaryTag": null,
                  "lineNumber": 3,
                  "projectName": null,
                  "fullFileName": "pom.xml",
                  "primaryTagValueAutoApplied": false
                }
              ],
              "count": 3,
              "responseCode": 200,
              "links": {
                "last": {
                  "href": "http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/116/issues?q=[issue_age]:updated&qm=issues&showhidden=false&showremoved=false&showsuppressed=false&start=0"
                },
                "first": {
                  "href": "http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/116/issues?q=[issue_age]:updated&qm=issues&showhidden=false&showremoved=false&showsuppressed=false&start=0"
                }
              }
            }\
            """;

    public static  final String issuesPart2 = """
            {
              "data": [
                {
                  "bugURL": null,
                  "hidden": false,
                  "issueName": "Issue 2",
                  "folderGuid": "bb824e8d-b401-40be-13bd-5d156696a686",
                  "lastScanId": 155,
                  "engineType": "SCA",
                  "issueStatus": "Unreviewed",
                  "friority": "Low",
                  "analyzer": "Configuration",
                  "primaryLocation": "pom.xml",
                  "reviewed": null,
                  "id": 3708,
                  "suppressed": false,
                  "hasAttachments": false,
                  "engineCategory": "STATIC",
                  "projectVersionName": null,
                  "removedDate": null,
                  "severity": 2.0,
                  "_href": "http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/116/issues/3708",
                  "displayEngineType": "SCA",
                  "foundDate": "2018-10-09T07:43:16.000+0000",
                  "confidence": 5.0,
                  "impact": 2.0,
                  "primaryRuleGuid": "FF57412F-DD28-44DE-8F4F-0AD39620768C",
                  "projectVersionId": 116,
                  "scanStatus": "UPDATED",
                  "audited": false,
                  "kingdom": "Environment",
                  "folderId": 215,
                  "revision": 0,
                  "likelihood": 0.8,
                  "removed": false,
                  "issueInstanceId": "87E3EC5CC8154C006783CC461A6DDEEB",
                  "hasCorrelatedIssues": false,
                  "primaryTag": null,
                  "lineNumber": 3,
                  "projectName": null,
                  "fullFileName": "pom.xml",
                  "primaryTagValueAutoApplied": false
                }
              ],
              "count": 3,
              "responseCode": 200,
              "links": {
                "last": {
                  "href": "http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/116/issues?q=[issue_age]:updated&qm=issues&showhidden=false&showremoved=false&showsuppressed=false&start=0"
                },
                "first": {
                  "href": "http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/116/issues?q=[issue_age]:updated&qm=issues&showhidden=false&showremoved=false&showsuppressed=false&start=0"
                }
              }
            }\
            """;

    public static  final String issuesPart3 = """
            {
              "data": [
                {
                  "bugURL": null,
                  "hidden": false,
                  "issueName": "Issue 3",
                  "folderGuid": "bb824e8d-b401-40be-13bd-5d156696a687",
                  "lastScanId": 155,
                  "engineType": "SCA",
                  "issueStatus": "Unreviewed",
                  "friority": "Low",
                  "analyzer": "Configuration",
                  "primaryLocation": "pom.xml",
                  "reviewed": null,
                  "id": 3708,
                  "suppressed": false,
                  "hasAttachments": false,
                  "engineCategory": "STATIC",
                  "projectVersionName": null,
                  "removedDate": null,
                  "severity": 2.0,
                  "_href": "http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/116/issues/3708",
                  "displayEngineType": "SCA",
                  "foundDate": "2018-10-09T07:43:16.000+0000",
                  "confidence": 5.0,
                  "impact": 2.0,
                  "primaryRuleGuid": "FF57412F-DD28-44DE-8F4F-0AD39620768C",
                  "projectVersionId": 116,
                  "scanStatus": "UPDATED",
                  "audited": false,
                  "kingdom": "Environment",
                  "folderId": 215,
                  "revision": 0,
                  "likelihood": 0.8,
                  "removed": false,
                  "issueInstanceId": "87E3EC5CC8154C006783CC461A6DDEEB",
                  "hasCorrelatedIssues": false,
                  "primaryTag": null,
                  "lineNumber": 3,
                  "projectName": null,
                  "fullFileName": "pom.xml",
                  "primaryTagValueAutoApplied": false
                }
              ],
              "count": 3,
              "responseCode": 200,
              "links": {
                "last": {
                  "href": "http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/116/issues?q=[issue_age]:updated&qm=issues&showhidden=false&showremoved=false&showsuppressed=false&start=0"
                },
                "first": {
                  "href": "http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/116/issues?q=[issue_age]:updated&qm=issues&showhidden=false&showremoved=false&showsuppressed=false&start=0"
                }
              }
            }\
            """;
}
