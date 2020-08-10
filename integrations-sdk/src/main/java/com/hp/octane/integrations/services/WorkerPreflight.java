package com.hp.octane.integrations.services;

import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.Map;

public class WorkerPreflight {


    private final static int QUEUE_EMPTY_INTERVAL = 10000;
    private final static int REGULAR_CYCLE_PAUSE = 250;
    private final static int NO_CONNECTION_PAUSE = 30000;
    private final static int AFTER_RECONNECTION_PAUSE = 60000;
    private final Object EMPTY_QUEUE_MONITOR = new Object();

    private ConfigurationService confService;
    private HasQueueService service;
    private Logger logger;

    private boolean previousIterationWasNotConnected;
    private long lastIterationTime = 0;
    private boolean waitAfterConnected = true;

    public WorkerPreflight(HasQueueService service, ConfigurationService confService, Logger logger) {
        this.service = service;
        this.logger = logger;
        this.confService = confService;
    }

    public boolean preflight() {

        lastIterationTime = System.currentTimeMillis();
        CIPluginSDKUtils.doWait(REGULAR_CYCLE_PAUSE);

        if (service.getQueueSize() == 0) {
            CIPluginSDKUtils.doBreakableWait(QUEUE_EMPTY_INTERVAL, EMPTY_QUEUE_MONITOR);
            return false;
        }

        if (confService.getConfiguration().isDisabled()) {
            logger.error(confService.getConfiguration().geLocationForLog() + "client is disabled, removing " + service.getQueueSize() + " items from queue");
            service.clearQueue();
            return false;
        }

        if (!confService.isConnected()) {
            //logger.warn(confService.getConfiguration().geLocationForLog() + "client is not connected. waiting " + NO_CONNECTION_PAUSE / 1000 + " sec");
            CIPluginSDKUtils.doWait(NO_CONNECTION_PAUSE);
            previousIterationWasNotConnected = true;
            return false;
        }
        if (previousIterationWasNotConnected && waitAfterConnected) {
            logger.warn(confService.getConfiguration().geLocationForLog() + "client is connected now. Giving time to events to be sent.");
            CIPluginSDKUtils.doWait(AFTER_RECONNECTION_PAUSE);
            previousIterationWasNotConnected = false;
            return false;
        }

        return true;
    }

    /**
     * Indicate preflight that item is added to queue, If preflight is waiting on emptyList, waiting time will be shortened.
     */
    public void itemAddedToQueue() {
        synchronized (EMPTY_QUEUE_MONITOR) {
            EMPTY_QUEUE_MONITOR.notify();
        }
    }

    public void addMetrics(Map<String, Object> metricsMap){
        metricsMap.put("lastIterationTime", new Date(lastIterationTime));
    }

    public void setWaitAfterConnection(boolean waitAfterConnected) {
        this.waitAfterConnected = waitAfterConnected;
    }
}
