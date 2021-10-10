package com.hp.octane.integrations.uft;

import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.uft.items.OctaneStatus;
import com.hp.octane.integrations.uft.items.SupportsOctaneStatus;
import com.hp.octane.integrations.uft.items.UftTestDiscoveryResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * @author Itay Karo on 26/08/2021
 */
public interface DiscoveryResultPreparer {
    Logger logger = LogManager.getLogger(UftTestDispatchUtils.class);

    void prepareDiscoveryResultForDispatchInFullSyncMode(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult);

    void prepareDiscoveryResultForDispatchInScmChangesMode(EntitiesService entitiesService, UftTestDiscoveryResult discoveryResult);

    default void removeItemsWithStatusNone(List<? extends SupportsOctaneStatus> list) {
        int counter = 0;
        for (int i = list.size(); i > 0; i--) {
            if (list.get(i - 1).getOctaneStatus().equals(OctaneStatus.NONE)) {
                list.remove(i - 1);
                counter++;
            }
        }

        logger.debug("total entities removed: {}", counter);
    }

}
