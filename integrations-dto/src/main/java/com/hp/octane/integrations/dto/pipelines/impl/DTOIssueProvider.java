package com.hp.octane.integrations.dto.pipelines.impl;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.DTOInternalProviderBase;
import com.hp.octane.integrations.dto.SecurityScans.OctaneIssue;
import com.hp.octane.integrations.dto.SecurityScans.impl.OctaneIssueImpl;

public class DTOIssueProvider  extends DTOInternalProviderBase {

    public DTOIssueProvider(DTOFactory.DTOConfiguration configuration) {
        super(configuration);
        dtoPairs.put(OctaneIssue.class, OctaneIssueImpl.class);
    }

    @Override
    protected <T extends DTOBase> T instantiateDTO(Class<T> targetType) throws InstantiationException, IllegalAccessException {
        T result = null;
        if (dtoPairs.containsKey(targetType)) {
            result = (T) dtoPairs.get(targetType).newInstance();
        }
        return result;
    }
}
