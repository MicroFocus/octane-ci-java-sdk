package com.hp.octane.integrations.services.vulnerabilities.fod.dto;

import java.util.function.Predicate;

/**
 * Created by hijaziy on 12/24/2017.
 */
public interface FODSource {
    <T extends FODEntityCollection> T getAllFODEntities(String rawURL, Class<T> targetClass, Predicate<T> whenToStopFetch);

    <T> T getSpeceficFODEntity(String rawURL, Class<T> targetClass);

    String getEntitiesURL();
}
