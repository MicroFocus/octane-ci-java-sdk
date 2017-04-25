package com.hp.octane.integrations.dto.executor.impl;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.DTOInternalProviderBase;
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.executor.TestSuiteExecutionInfo;
import com.hp.octane.integrations.dto.executor.TestExecutionInfo;

/**
 */

public final class DTOExecutorsProvider extends DTOInternalProviderBase {

    public DTOExecutorsProvider(DTOFactory.DTOConfiguration configuration) {
        dtoPairs.put(DiscoveryInfo.class, DiscoveryInfoImpl.class);
        dtoPairs.put(TestSuiteExecutionInfo.class, TestSuiteExecutionInfoImpl.class);
        dtoPairs.put(TestExecutionInfo.class, TestExecutionInfoImpl.class);
        dtoPairs.put(TestConnectivityInfo.class, TestConnectivityInfoImpl.class);
    }

    protected <T extends DTOBase> T instantiateDTO(Class<T> targetType) throws InstantiationException, IllegalAccessException {
        T result = null;
        if (dtoPairs.containsKey(targetType)) {
            result = (T) dtoPairs.get(targetType).newInstance();
        }
        return result;
    }
}
