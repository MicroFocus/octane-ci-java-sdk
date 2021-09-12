package com.hp.octane.integrations.uft;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.uft.items.CustomLogger;
import com.hp.octane.integrations.uft.items.JobRunContext;
import com.hp.octane.integrations.uft.items.UftTestDiscoveryResult;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.util.Collections;

/**
 * @author Itay Karo on 26/08/2021
 */
public abstract class DiscoveryResultDispatcher {

    static final DTOFactory dtoFactory = DTOFactory.getInstance();

    abstract void dispatchDiscoveryResults(EntitiesService entitiesService, UftTestDiscoveryResult result, JobRunContext jobRunContext, CustomLogger customLogger);

    void logMessage(Logger logger, Level level, CustomLogger customLogger, String msg) {
        logger.log(level, msg);
        if (customLogger != null) {
            try {
                customLogger.add(msg);
            } catch (Exception e) {
                logger.error("failed to add to customLogger " + e.getMessage());
            }
        }
    }

    static Entity createListNodeEntity(String id) {
        return dtoFactory.newDTO(Entity.class).setType("list_node").setId(id);
    }

    static Entity createModelItemEntity(Entity modelItemEntity) {
        return dtoFactory.newDTO(Entity.class).setField("data", Collections.singletonList(modelItemEntity));
    }

}
