package com.hp.octane.integrations.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
/**
 * Created by lazara on 08/06/2017.
 */
public class CIPluginUtils {

    public static boolean isNonProxyHost(String targetHost, String nonProxyHostsStr){

        boolean noProxyHost = false;
        for (Pattern pattern : getNoProxyHostPatterns(nonProxyHostsStr)) {
            if (pattern.matcher(targetHost).find()) {
                noProxyHost = true;
                break;
            }
        }
        return noProxyHost;
    }

    private static List<Pattern> getNoProxyHostPatterns(String noProxyHost) {
        if(noProxyHost == null ||noProxyHost.isEmpty()) {
            return Collections.emptyList();
        } else {
            ArrayList r = new ArrayList();
            String[] arr$ = noProxyHost.split("[ \t\n,|]+");
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                String s = arr$[i$];
                if(s.length() != 0) {
                    r.add(Pattern.compile(s.replace(".", "\\.").replace("*", ".*")));
                }
            }

            return r;
        }
    }
}

