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
package com.hp.octane.integrations.uft.ufttestresults.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UftResultStepData implements Serializable {
    private List<String> parents;
    private String type;
    private String result;
    private String message;
    private long duration;
    private List<UftResultStepParameter> inputParameters = new ArrayList<>();
    private List<UftResultStepParameter> outputParameters = new ArrayList<>();

    public UftResultStepData(List<String> parents, String type, String result, String message, long duration) {
        this.parents = parents;
        this.type = type;
        this.result = result;
        this.message = message;
        this.duration = duration;
    }

    public UftResultStepData(List<String> parents, String type, String result, String message, long duration, List<UftResultStepParameter> inputParameters, List<UftResultStepParameter> outputParameters) {
        this.parents = parents;
        this.type = type;
        this.result = result;
        this.message = message;
        this.duration = duration;
        this.inputParameters = inputParameters;
        this.outputParameters = outputParameters;
    }

    public List<String> getParents() {
        return parents;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getResult() {
        return result;
    }

    public String toString() {
        return String.join("/", parents) + ":" + result + (message != null ? "," + message : "");
    }

    public long getDuration() {
        return duration;
    }

    public List<UftResultStepParameter> getInputParameters() {
        return inputParameters;
    }

    public List<UftResultStepParameter> getOutputParameters() {
        return outputParameters;
    }
}
