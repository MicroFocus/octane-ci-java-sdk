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
package com.hp.octane.integrations.uft.ufttestresults;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hp.octane.integrations.dto.tests.TestRunResult;
import com.hp.octane.integrations.uft.ufttestresults.schema.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class UftTestResultsUtils {

    private static Set<String> notFinalNodeType = new HashSet<>(Arrays.asList("Iteration", "Action", "Context"));

    public static List<UftResultStepData> getErrorData(File file) {
        ReportResults result = fromXml(file, ReportResults.class);
        List<UftResultStepData> errors = new ArrayList<>();
        List<String> parents = new ArrayList<>();
        getErrorDataInternal(result.getReportNode(), parents, errors);
        return errors;
    }

    public static String getAggregatedErrorMessage(List<UftResultStepData> errors) {
        String err = errors.stream()
                .map(e -> e.getMessage().trim() + ((e.getResult().equalsIgnoreCase("Warning")) ? " (Warning)" : ""))
                .map(msg -> msg + (msg.endsWith(".") ? "" : ". "))
                .distinct()
                .collect(Collectors.joining("\n"));
        return err;
    }

    public static List<UftResultIterationData> getMBTData(File file) {
        ReportResults result = fromXml(file, ReportResults.class);
        final ArrayList<ReportNode> iterationReportNodes = new ArrayList<>();
        getMBTIterationsInternal(result.getReportNode(), new ArrayList<>(), iterationReportNodes, "Iteration", 2);
        List<UftResultIterationData> iterations = new ArrayList<>();
        iterationReportNodes.forEach(reportNode -> {
            List<UftResultStepData> steps = new ArrayList<>();
            getMBTDataInternal(reportNode, new ArrayList<>(), steps, "Action", 3);
            iterations.add(new UftResultIterationData(steps, reportNode.getData().getDuration()));
        });
        return iterations;
    }

    private static void getErrorDataInternal(ReportNode node, List<String> parents, List<UftResultStepData> errors) {
        parents.add(node.getData().getName());
        boolean hasDescription = isNotEmpty(node.getData().getDescription());
        boolean failed = "Failed".equalsIgnoreCase(node.getData().getResult()) || "Warning".equalsIgnoreCase(node.getData().getResult());
        if (failed) {
            if (!notFinalNodeType.contains(node.getType()) && hasDescription) {
                //String parentStr = String.join("/", parents.subList(1, parents.size()));
                String error = /*parentStr + ":" +*/ (isNotEmpty(node.getData().getErrorText()) ? node.getData().getErrorText() : node.getData().getDescription());
                error = error.replace("Verify that this object's properties match an object currently displayed in your application.", "")
                        .replace("\n", "")
                        .replace("&nbsp;", " ")
                        .trim();

                //last parent name might be as error message - in this case - don't show last parent
                if (!parents.isEmpty() && error.startsWith(parents.get(parents.size() - 1))) {
                    parents.remove(parents.size() - 1);
                }
                errors.add(new UftResultStepData(parents, node.getType(), node.getData().getResult(), error, node.getData().getDuration()));
            }
            if (node.getNodes() != null) {
                node.getNodes().forEach(n -> getErrorDataInternal(n, new ArrayList<>(parents), errors));
            }
        }
    }

    private static void getMBTIterationsInternal(ReportNode node, List<String> parents, List<ReportNode> results, String nodeType, int targetLevel) {
        parents.add(node.getData().getName());

        if (parents.size() == targetLevel && nodeType.equals(node.getType())) {
            results.add(node);
        }

        if (node.getNodes() != null && parents.size() < targetLevel) {
            node.getNodes().forEach(n -> getMBTIterationsInternal(n, new ArrayList<>(parents), results, nodeType, targetLevel));
        }
    }

    private static void getMBTDataInternal(ReportNode node, List<String> parents, List<UftResultStepData> results, String nodeType, int targetLevel) {
        parents.add(node.getData().getName());

        boolean failed = "Failed".equalsIgnoreCase(node.getData().getResult()) || "Warning".equalsIgnoreCase(node.getData().getResult());


        if (parents.size() == targetLevel && nodeType.equals(node.getType())) {

            String errorMessage = "";
            if (failed) {

                List<UftResultStepData> errors = new ArrayList<>();
                getErrorDataInternal(node, new ArrayList(parents), errors);
                errorMessage = getAggregatedErrorMessage((errors));
            }

            List<UftResultStepParameter> inputParameters = null;
            if (node.getData().getInputParameters() != null) {
                inputParameters = node.getData().getInputParameters().stream().map(parameter -> new UftResultStepParameter(parameter.getName(), parameter.getValue(), parameter.getType())).collect(Collectors.toList());
            }
            List<UftResultStepParameter> outputParameters = null;
            if (node.getData().getOutputParameters() != null) {
                outputParameters = node.getData().getOutputParameters().stream().map(parameter -> new UftResultStepParameter(parameter.getName(), parameter.getValue(), parameter.getType())).collect(Collectors.toList());
            }
            String result = node.getData().getResult().equalsIgnoreCase("Warning") ? TestRunResult.PASSED.value() : node.getData().getResult();
            results.add(new UftResultStepData(parents, node.getType(), result, errorMessage, node.getData().getDuration(), inputParameters, outputParameters));
        }

        if (node.getNodes() != null && parents.size() < targetLevel) {
            node.getNodes().forEach(n -> getMBTDataInternal(n, new ArrayList<>(parents), results, nodeType, targetLevel));
        }
    }

    private static boolean isNotEmpty(String str) {
        return !(str == null || str.isEmpty());
    }

    public static <T> T fromXml(File xml, Class<T> clazz) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            T obj = xmlMapper.readValue(xml, clazz);

            return obj;
        } catch (Exception e) {
            throw new IllegalStateException("Error while deserializing a XML file to Object of type " + clazz, e);
        }
    }
}
