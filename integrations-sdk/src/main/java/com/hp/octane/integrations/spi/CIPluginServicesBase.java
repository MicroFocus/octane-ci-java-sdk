package com.hp.octane.integrations.spi;

import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.executor.TestSuiteExecutionInfo;
import com.hp.octane.integrations.dto.general.CIJobsList;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.dto.pipelines.BuildHistory;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.snapshots.SnapshotNode;
import com.hp.octane.integrations.dto.tests.TestsResult;

import java.io.File;

/**
 * Empty implementation of CIPluginServices.
 */

public class CIPluginServicesBase implements CIPluginServices {


    @Override
    public CIServerInfo getServerInfo() {
        return null;
    }

    @Override
    public CIPluginInfo getPluginInfo() {
        return null;
    }

    @Override
    public File getAllowedOctaneStorage() {
        return null;
    }

    @Override
    public File getPredictiveOctanePath() {
        return null;
    }

    @Override
    public OctaneConfiguration getOctaneConfiguration() {
        return null;
    }

    @Override
    public CIProxyConfiguration getProxyConfiguration(String targetHost) {
        return null;
    }

    @Override
    public CIJobsList getJobsList(boolean includeParameters) {
        return null;
    }

    @Override
    public PipelineNode getPipeline(String rootCIJobId) {
        return null;
    }

    @Override
    public void runPipeline(String ciJobId, String originalBody) {
        //do nothing
    }

    @Override
    public SnapshotNode getSnapshotLatest(String ciJobId, boolean subTree) {
        return null;
    }

    @Override
    public SnapshotNode getSnapshotByNumber(String ciJobId, String buildCiId, boolean subTree) {
        return null;
    }

    @Override
    public BuildHistory getHistoryPipeline(String ciJobId, String originalBody) {
        return null;
    }

    @Override
    public TestsResult getTestsResult(String jobId, String buildNumber) {
        return null;
    }

    @Override
    public void runTestDiscovery(DiscoveryInfo discoveryInfo) {
        //do nothing
    }

    @Override
    public void runTestSuiteExecution(TestSuiteExecutionInfo testSuiteExecutionInfo) {
        //do nothing
    }

    @Override
    public boolean checkRepositoryConnectivity(TestConnectivityInfo testConnectivityInfo) {
        //do nothing
        return false;
    }

    @Override
    public void deleteSuite(String id) {

    }

    @Override
    public void deleteExecutor(String id) {

    }

}
