package com.hp.octane.integrations.services.vulnerabilities.fod.dto.services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by hijaziy on 8/6/2017.
 */
public class FODToLocalServiceTimeSrvice {

    //TODO: FOD time is UTC, right?
    // their time values are "Z" , ZULU times.
    public static Long getUTCMilliesFODTime(String dateTimeValue){

        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            Date parse = format.parse(dateTimeValue);
            return parse.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

    }

}
