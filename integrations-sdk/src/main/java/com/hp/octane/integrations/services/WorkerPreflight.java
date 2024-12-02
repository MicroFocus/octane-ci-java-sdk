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
package com.hp.octane.integrations.services;

import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.Map;

public class WorkerPreflight {


    private final static int QUEUE_EMPTY_INTERVAL = 10000;
    private final static int REGULAR_CYCLE_PAUSE = 400;
    private final static int NO_CONNECTION_PAUSE = 30000;
    private final static int AFTER_RECONNECTION_PAUSE = 90000;
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
            logger.error(confService.getConfiguration().getLocationForLog() + "client is disabled, removing " + service.getQueueSize() + " items from queue");
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
            logger.warn(confService.getConfiguration().getLocationForLog() + "client is connected now. Giving time to events to be sent.");
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
