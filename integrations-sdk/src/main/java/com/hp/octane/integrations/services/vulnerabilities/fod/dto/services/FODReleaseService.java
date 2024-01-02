/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
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

package com.hp.octane.integrations.services.vulnerabilities.fod.dto.services;

import com.hp.octane.integrations.services.vulnerabilities.fod.dto.FodConnectionFactory;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.pojos.Scan;

import java.util.List;
import java.util.function.Predicate;

/**
 * Created by hijaziy on 8/3/2017.
 */
public class FODReleaseService {

	//https://api.sandbox.fortify.com/api/v3/releases/991/scans
	static final String urlScansFormat = "%s/releases/%d/scans?orderBy=startedDateTime&orderByDirection=DESC";

	static final String urlScanFormatById = "%s/releases/%d/scans/%d";

	public static List<Scan> getScansLastInFirstFetched(Long releaseId, Long relevanceTime) {

		String url = String.format(urlScansFormat, FodConnectionFactory.instance().getEntitiesURL(), releaseId);

		Predicate<Scan.Scans> stopFetching = null;
		if (relevanceTime != null) {
			stopFetching = (t) -> t.items.stream().anyMatch((scan -> {

				Long milliesFODTime = FODToLocalServiceTimeSrvice.getUTCMilliesFODTime(scan.startedDateTime);
				return milliesFODTime < relevanceTime;
			}
			));
		}
		Scan.Scans allFODEntities = FodConnectionFactory.instance().getAllFODEntities(url, Scan.Scans.class, stopFetching);
		return allFODEntities.items;
	}


	public static Scan getCompleteScan(Long releaseId, Long relevantScanId) {
		String url = String.format(urlScanFormatById, FodConnectionFactory.instance().getEntitiesURL(), releaseId, relevantScanId);
		return FodConnectionFactory.instance().getSpeceficFODEntity(url, Scan.class);
	}
}
