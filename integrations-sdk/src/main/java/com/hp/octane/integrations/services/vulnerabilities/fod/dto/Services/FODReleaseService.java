package com.hp.octane.integrations.services.vulnerabilities.fod.dto.Services;

import com.hp.octane.integrations.services.vulnerabilities.fod.dto.FodConnectionFactory;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.POJOs.Scan;

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
