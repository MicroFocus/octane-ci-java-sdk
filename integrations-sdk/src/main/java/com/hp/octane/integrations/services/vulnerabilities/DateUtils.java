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
 *
 */
package com.hp.octane.integrations.services.vulnerabilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {

    public static final String sscFormat = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String sonarFormat = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String octaneFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private final static Logger logger = LogManager.getLogger(DateUtils.class);

    public static String convertDateSSCToOctane(String inputFoundDate, String srcFormat) {
        if (inputFoundDate == null) {
            return null;
        }
        Date date = getDateFromUTCString(inputFoundDate, srcFormat);
        SimpleDateFormat targetDateFormat = new SimpleDateFormat(octaneFormat);
        return targetDateFormat.format(date);
    }

    public static Date getDateFromUTCString(String inputDate, String format) {
        try {
            DateFormat sourceDateFormat = new SimpleDateFormat(format);
            sourceDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sourceDateFormat.parse(inputDate);
        } catch (ParseException e) {
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
            return null;
        }
    }
}
