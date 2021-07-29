/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.hp.octane.integrations.uft.items;

import java.io.Serializable;

/**
 * This file represents automated test action for sending to Octane
 */

public class UftTestAction implements SupportsOctaneStatus, Serializable {

    private String name;

    private String logicalName;

    private String repositoryPath;

    private OctaneStatus octaneStatus;

    public UftTestAction() {
        this.octaneStatus = OctaneStatus.NEW;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName = logicalName;
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    @Override
    public OctaneStatus getOctaneStatus() {
        return octaneStatus;
    }

    public void setOctaneStatus(OctaneStatus octaneStatus) {
        this.octaneStatus = octaneStatus;
    }

}
