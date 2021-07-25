package com.hp.octane.integrations.uft.ufttestresults;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hp.octane.integrations.uft.ufttestresults.schema.ReportNode;
import com.hp.octane.integrations.uft.ufttestresults.schema.ReportResults;
import com.hp.octane.integrations.uft.ufttestresults.schema.UftErrorData;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class UftTestResultsUtils {

    private static Set<String> notFinalNodeType = new HashSet<>(Arrays.asList("Iteration", "Action", "Context"));

    public static List<UftErrorData> getErrorData(File file) {
        ReportResults result = fromXml(file, ReportResults.class);
        List<UftErrorData> errors = new ArrayList<>();
        List<String> parents = new ArrayList<>();
        iterate(result.getReportNode(), parents, errors);
        return errors;
    }

    public static String getAggregatedErrorMessage(List<UftErrorData> errors) {
        String err = errors.stream()
                .map(e -> e.getMessage().trim() + ((e.getResult().equalsIgnoreCase("Warning")) ? " (Warning)" : ""))
                .map(msg -> msg + (msg.endsWith(".") ? "" : ". "))
                .distinct()
                .collect(Collectors.joining("\n"));
        return err;
    }

    private static void iterate(ReportNode node, List<String> parents, List<UftErrorData> errors) {
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
                errors.add(new UftErrorData(parents, node.getType(), node.getData().getResult(), error));
            }
            if (node.getNodes() != null) {
                node.getNodes().forEach(n -> iterate(n, new ArrayList<>(parents), errors));
            }
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
