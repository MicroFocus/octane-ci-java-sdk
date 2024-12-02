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
package com.hp.octane.integrations.uft.items;

import java.io.Serializable;
import java.util.List;

/**
 * This file represents automated test for sending to Octane
 */

public class AutomatedTest implements SupportsMoveDetection, SupportsOctaneStatus, Serializable {

    private String id;

    private String changeSetSrc;

    private String changeSetDst;

    private String oldName;

    private String oldPackageName;

    private Boolean isMoved;

    private UftTestType uftTestType;

    private OctaneStatus octaneStatus;

    private boolean missingScmRepository;

    private boolean missingTestRunner;

    private String name;

    private String packageName;

    private Boolean executable;

    private String description;

    private List<UftTestAction> actions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackage() {
        return packageName;
    }

    public void setPackage(String packageName) {
        this.packageName = packageName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUftTestType(UftTestType uftTestType) {
        this.uftTestType = uftTestType;
    }

    public UftTestType getUftTestType() {
        return uftTestType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getExecutable() {
        return executable;
    }

    public void setExecutable(Boolean executable) {
        this.executable = executable;
    }

    @Override
    public String toString() {
        return "#" + (getId() == null ? "0" : getId()) + " - " + getPackage() + "@" + getName();
    }

    @Override
    public String getChangeSetSrc() {
        return changeSetSrc;
    }

    @Override
    public void setChangeSetSrc(String changeSetSrc) {
        this.changeSetSrc = changeSetSrc;
    }

    @Override
    public String getChangeSetDst() {
        return changeSetDst;
    }

    @Override
    public void setChangeSetDst(String changeSetDst) {
        this.changeSetDst = changeSetDst;
    }

    public String getOldName() {
        return oldName;
    }

    public void setOldName(String oldName) {
        this.oldName = oldName;
    }

    public String getOldPackage() {
        return oldPackageName;
    }

    public void setOldPackage(String oldPackageName) {
        this.oldPackageName = oldPackageName;
    }

    public Boolean getIsMoved() {
        return isMoved != null ? isMoved : false;
    }

    public void setIsMoved(Boolean moved) {
        isMoved = moved;
    }

    @Override
    public OctaneStatus getOctaneStatus() {
        return octaneStatus;
    }

    public void setOctaneStatus(OctaneStatus octaneStatus) {
        this.octaneStatus = octaneStatus;
    }

    public boolean isMissingScmRepository() {
        return missingScmRepository;
    }

    public void setMissingScmRepository(boolean missingScmRepository) {
        this.missingScmRepository = missingScmRepository;
    }

    public boolean isMissingTestRunner() {
        return missingTestRunner;
    }

    public void setMissingTestRunner(boolean missingTestRunner) {
        this.missingTestRunner = missingTestRunner;
    }

    public List<UftTestAction> getActions() {
        return actions;
    }

    public void setActions(List<UftTestAction> actions) {
        this.actions = actions;
    }
}
