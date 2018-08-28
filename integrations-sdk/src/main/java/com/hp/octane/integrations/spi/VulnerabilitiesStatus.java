package com.hp.octane.integrations.spi;

import java.io.InputStream;

public class VulnerabilitiesStatus {

    public Polling polling;
    public InputStream issuesStream;

    public VulnerabilitiesStatus(Polling polling, InputStream inputStream){
        this.polling = polling;
        this.issuesStream = inputStream;
    }
    public static enum Polling{
        ContinuePolling,
        ScanIsCompleted,
        StopPolling
    }
}
