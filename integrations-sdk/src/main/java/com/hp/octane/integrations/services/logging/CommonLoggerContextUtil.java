package com.hp.octane.integrations.services.logging;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class CommonLoggerContextUtil {

    private static final Object INIT_LOCKER = new Object();
    private static LoggerContext commonLoggerContext;
    private static final String OCTANE_ALLOWED_STORAGE_LOCATION = "octaneAllowedStorage";


    public static LoggerContext configureLogger(File allowedStorage) {
        if (allowedStorage != null && (allowedStorage.isDirectory() || !allowedStorage.exists())) {
            synchronized (INIT_LOCKER) {
                if (commonLoggerContext != null) {
                    if (!commonLoggerContext.isStarted()) {
                        commonLoggerContext.start();
                    }
                    return commonLoggerContext;
                }
                commonLoggerContext = LoggerContext.getContext(false);

                if (!commonLoggerContext.isStarted()) {
                    commonLoggerContext.start();
                }
                System.setProperty(OCTANE_ALLOWED_STORAGE_LOCATION, allowedStorage.getAbsolutePath() + File.separator);

                //case for team city, that cannot find log4j
                if (commonLoggerContext.getConfiguration() == null ||
                        !(commonLoggerContext.getConfiguration() instanceof XmlConfiguration)) {

                    URL path = LoggingServiceImpl.class.getClassLoader().getResource("log4j2.xml");
                    if (path != null) {
                        try {
                            commonLoggerContext.setConfigLocation(path.toURI());
                            //reconfigure is called inside
                        } catch (URISyntaxException e) {
                            //failed to convert to URI
                        }
                    }
                }

                commonLoggerContext.reconfigure();
            }
        }
        return commonLoggerContext;
    }
}
