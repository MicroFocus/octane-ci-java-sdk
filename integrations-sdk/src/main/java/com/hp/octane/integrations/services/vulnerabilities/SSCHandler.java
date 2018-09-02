package com.hp.octane.integrations.services.vulnerabilities;


import com.hp.octane.integrations.api.ConfigurationService;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.services.vulnerabilities.ssc.*;
import com.hp.octane.integrations.spi.VulnerabilitiesStatus;
//import com.hpe.application.automation.tools.ssc.*;
//import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
//import com.microfocus.application.automation.tools.sse.common.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by hijaziy on 7/23/2018.
 */
public class SSCHandler {

    private final static Logger logger = LogManager.getLogger(SSCHandler.class);
    private SscProjectConnector sscProjectConnector;
    private ProjectVersions.ProjectVersion projectVersion;
    private String targetDir;
    private long runStartTime;


    public static final String SCAN_RESULT_FILE = "securityScan.json";
    public static String SEVERITY_LG_NAME_LOW = "list_node.severity.low";
    public static String SEVERITY_LG_NAME_MEDIUM = "list_node.severity.medium";
    public static String SEVERITY_LG_NAME_HIGH = "list_node.severity.high";
    public static String SEVERITY_LG_NAME_CRITICAL = "list_node.severity.urgent";
    public static String EXTERNAL_TOOL_NAME =  "Fortify SSC";
    public static String ARTIFACT_STATUS_COMPLETE = "PROCESS_COMPLETE";
    public static String ARTIFACT_ERROR_PROCESSING = "ERROR_PROCESSING";



    public VulnerabilitiesStatus.Polling getScanFinishStatus() {
        logger.debug("enter getScanFinishStatus");

        Artifacts artifacts = sscProjectConnector.getArtifactsOfProjectVersion(this.projectVersion.id, 10);
        Artifacts.Artifact closestArtifact = getClosestArtifact(artifacts);
        if(closestArtifact == null){
            logger.debug("Cannot find artifact of the run");
            return VulnerabilitiesStatus.Polling.ContinuePolling;
        }
        if(closestArtifact.status.equals(ARTIFACT_STATUS_COMPLETE)){
            logger.debug("artifact of the run is in completed");
            return VulnerabilitiesStatus.Polling.ScanIsCompleted;
        }
        if(closestArtifact.status.equals(ARTIFACT_ERROR_PROCESSING)){
            logger.debug("artifact of the run faced error, polling should stop");
            return VulnerabilitiesStatus.Polling.StopPolling;
        }
        logger.debug("artifact of the run is not complete, polling should continue");
        return VulnerabilitiesStatus.Polling.ContinuePolling;
    }


    private Artifacts.Artifact getClosestArtifact(Artifacts artifacts) {
        Artifacts.Artifact theCloset = null;
        if(artifacts == null ||
                artifacts.data == null){
            return null;
        }
        Date startRunDate = new Date(this.runStartTime);

        for (Artifacts.Artifact artifact : artifacts.data) {
            Date uploadDate = SSCDateUtils.getDateFromUTCString(artifact.uploadDate, SSCDateUtils.sscFormat);
            if(uploadDate.after(startRunDate)){
                theCloset = artifact;
            }
        }
        return theCloset;

    }

    public SSCHandler(VulnerabilitiesServiceImpl.VulnerabilitiesQueueItem vulnerabilitiesQueueItem, String sscUrl, String sscBaseToken) {

        //"Basic QWRtaW46ZGV2c2Vjb3Bz"
        SSCFortifyConfigurations sscFortifyConfigurations = new SSCFortifyConfigurations();
        sscFortifyConfigurations.baseToken = sscBaseToken;
        sscFortifyConfigurations.projectName = vulnerabilitiesQueueItem.projectName;
        sscFortifyConfigurations.projectVersion = vulnerabilitiesQueueItem.projectVersionSymbol;
        sscFortifyConfigurations.serverURL = sscUrl;

        this.targetDir = vulnerabilitiesQueueItem.targetFolder;
        this.runStartTime = vulnerabilitiesQueueItem.startTime;

        if(StringUtils.isNullOrEmpty(sscFortifyConfigurations.baseToken)||
                StringUtils.isNullOrEmpty(sscFortifyConfigurations.projectName)||
                StringUtils.isNullOrEmpty(sscFortifyConfigurations.projectVersion)||
                StringUtils.isNullOrEmpty(sscFortifyConfigurations.serverURL)){
            logger.warn("missing one of the SSC configuration fields (baseToken\\project\\version\\serverUrl) will not continue connecting to the server");
        }else {
            sscProjectConnector = SSCClientManager.instance().getProjectConnector(sscFortifyConfigurations);
            if (sscProjectConnector != null) {
                projectVersion = sscProjectConnector.getProjectVersion();
            }
        }

    }

    public boolean isConnected(){
        return sscProjectConnector !=null;
    }

    public void getLatestScan() {
        logger.warn("entered getLatestScan, read issues and serialize to:" + this.targetDir);
        Issues issues = sscProjectConnector.readNewIssuesOfLastestScan(projectVersion.id);
        List<OctaneIssue> octaneIssues = createOctaneIssues(issues);
        IssuesFileSerializer issuesFileSerializer = new IssuesFileSerializer(targetDir, octaneIssues);
        issuesFileSerializer.doSerialize();
        logger.warn("exit getLatestScan");
    }

    private List<OctaneIssue> createOctaneIssues(Issues issues) {
        if(issues == null){
            return new ArrayList<>();
        }
        DTOFactory dtoFactory = DTOFactory.getInstance();
        List<OctaneIssue> octaneIssues = new ArrayList<>();
        for (Issues.Issue issue : issues.data) {
            OctaneIssue octaneIssue = createOctaneIssue(dtoFactory, issue);
            octaneIssues.add(octaneIssue);
        }

        return octaneIssues;
    }

    private OctaneIssue createOctaneIssue(DTOFactory dtoFactory, Issues.Issue issue) {
        logger.debug("enter createOctaneIssue");
        OctaneIssue octaneIssue = dtoFactory.newDTO(OctaneIssue.class);
        setOctaneAnalysis(dtoFactory, issue, octaneIssue);
        setOctaneSeverity(dtoFactory, issue, octaneIssue);
        setOctaneStatus(dtoFactory, issue, octaneIssue);
        Map extendedData = getExtendedData(issue);
        octaneIssue.setExtendedData(extendedData);
        octaneIssue.setPrimaryLocationFull(issue.primaryLocation);
        octaneIssue.setLine(issue.lineNumber);
        octaneIssue.setRemoteId(issue.issueInstanceId);
        octaneIssue.setIntroducedDate(convertDates(issue.foundDate));
        octaneIssue.setExternalLink(issue.hRef);
        octaneIssue.setToolName(EXTERNAL_TOOL_NAME);
        logger.debug("exit createOctaneIssue");
        return octaneIssue;
    }

    static private String convertDates(String inputFoundDate) {
        if(inputFoundDate == null){
            return null;
        }
        //"2017-02-12T12:31:44.000+0000"
        DateFormat sourceDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        Date date = null;
        try {
            date = sourceDateFormat.parse(inputFoundDate);
            //"2018-06-03T14:06:58Z"
            SimpleDateFormat targetDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            return targetDateFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setOctaneAnalysis(DTOFactory dtoFactory, Issues.Issue issue, OctaneIssue octaneIssue) {
//        "issueStatus" : "Unreviewed", - analysis
//        "audited" : false,- analysis
//        "reviewed" : null, - analysis ?

//        "list_node.issue_analysis_node.not_an_issue"
//        "list_node.issue_analysis_node.maybe_an_issue"
//        "list_node.issue_analysis_node.bug_submitted"
//        "list_node.issue_analysis_node.reviewed"
        if (isReviewed(issue)) {
            octaneIssue.setAnalysis(createListNodeEntity(dtoFactory, "list_node.issue_analysis_node.reviewed"));
        }
    }

    private boolean isReviewed(Issues.Issue issue) {
        boolean returnValue = false;
        if (issue.issueStatus != null && issue.issueStatus.equalsIgnoreCase("reviewed")) {
            returnValue = true;
        } else if (issue.reviewed != null && issue.reviewed) {
            returnValue = true;
        } else if (issue.audited != null && issue.audited) {
            returnValue = true;
        }
        return returnValue;
    }

    private void setOctaneStatus(DTOFactory dtoFactory, Issues.Issue issue, OctaneIssue octaneIssue) {
        if (issue.scanStatus != null) {
            String listNodeId = null;
            if(issue.scanStatus.toLowerCase().equalsIgnoreCase("updated")) {
                listNodeId = "list_node.issue_state_node.existing";
            }
            else if(issue.scanStatus.toLowerCase().equalsIgnoreCase("new")) {
                listNodeId = "list_node.issue_state_node.new";
            }
            if (isLegalOctaneState(listNodeId)) {
                octaneIssue.setState(createListNodeEntity(dtoFactory, listNodeId));
            }
        }
        if(issue.removed != null && issue.removed){
            octaneIssue.setState(createListNodeEntity(dtoFactory, "list_node.issue_state_node.closed"));
        }
    }

    private boolean isLegalOctaneState(String scanStatus) {
        List<String> legalNames = Arrays.asList("list_node.issue_state_node.new",
                "list_node.issue_state_node.existing",
                "list_node.issue_state_node.closed",
                "list_node.issue_state_node.reopen");
        return (legalNames.contains(scanStatus));
    }

    private Map getExtendedData(Issues.Issue issue) {
        Map retVal = new HashMap();
        retVal.put("issueName", issue.issueName);
        retVal.put("likelihood", issue.likelihood);
        retVal.put("kingdom", issue.kingdom);
        retVal.put("impact", issue.impact);
        retVal.put("confidence", issue.confidance);
        retVal.put("removedDate", issue.removedDate);
        return retVal;
    }

    private void setOctaneSeverity(DTOFactory dtoFactory, Issues.Issue issue, OctaneIssue octaneIssue) {
        if (issue.severity != null) {
            String octaneSeverity = getOctaneSeverityFromSSCValue(issue.severity);
            octaneIssue.setSeverity(createListNodeEntity(dtoFactory, octaneSeverity));
        }
    }
    private String getOctaneSeverityFromSSCValue(String severity) {
        if (severity == null) {
            return null;
        }
        String logicalNameForSeverity = null;
        if (severity.startsWith("4")) {
            logicalNameForSeverity = SEVERITY_LG_NAME_CRITICAL;
        }
        if (severity.startsWith("3")) {
            logicalNameForSeverity = SEVERITY_LG_NAME_HIGH;
        }
        if (severity.startsWith("2")) {
            logicalNameForSeverity = SEVERITY_LG_NAME_MEDIUM;
        }
        if (severity.startsWith("1")) {
            logicalNameForSeverity = SEVERITY_LG_NAME_LOW;
        }

        return logicalNameForSeverity;

    }
    private static Entity createListNodeEntity(DTOFactory dtoFactory, String id) {
        if(id == null){
            return null;
        }
        return dtoFactory.newDTO(Entity.class).setType("list_node").setId(id);
    }


    private void saveReport() {

        String vulnerabilitiesScanFilePath = new File(this.targetDir, SCAN_RESULT_FILE).getPath();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        System.out.println(timeStamp+" : working on : "+vulnerabilitiesScanFilePath);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String yamin = getYaminVul();
        try (PrintWriter out = new PrintWriter(vulnerabilitiesScanFilePath)) {
//            out.write(yamin);
            out.write(getVulString());

            out.flush();
            out.close();
//        } catch (com.hp.mqm.client.exception.FileNotFoundException e) {
//            e.printStackTrace();
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String getYaminVul() {
        return "{\n" +
                "\t\"data\": [{\n" +
                "\t\t\"primary_location_full\": \"hellow.java\",\n" +
                "\t\t\"line\": 5,\n" +
                "\t\t\"analysis\": {\n" +
                "\t\t\t\"id\": \"list_node.issue_analysis_node.reviewed\",\n" +
                "\t\t\t\"type\": \"list_node\"\n" +
                "\t\t},\n" +
                "\t\t\"state\": {\n" +
                "\t\t\t\"id\": \"list_node.issue_state_node.new\",\n" +
                "\t\t\t\"type\": \"list_node\"\n" +
                "\t\t},\n" +
                "\t\t\"severity\": {\n" +
                "\t\t\t\"id\": \"list_node.severity.medium\",\n" +
                "\t\t\t\"type\": \"list_node\"\n" +
                "\t\t},\n" +
                "\t\t\"remote_id\": \"98F2CC18089300065BC94822FC4AD02B\",\n" +
                "\t\t\"introduced_date\": \"2017-02-12T12:31:44.000+0000\",\n" +
                "\t\t\"external_link\": \"http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/8/issues/2743\",\n" +
                "\t\t\"extended_data\": {\n" +
                "\t\t\t\"issueName\": \"J2EE Bad Practices: Leftover Debug Code\",\n" +
                "\t\t\t\"likelihood\": \"0.8\",\n" +
                "\t\t\t\"impact\": \"2.0\",\n" +
                "\t\t\t\"confidence\": \"5.0\",\n" +
                "\t\t\t\"kingdom\": \"Encapsulation\",\n" +
                "\t\t\t\"removedDate\": null\n" +
                "\t\t},\n" +
                "\t\t\"tool_name\": \"external tool\"\n" +
                "\t},\n" +
                "\t{\n" +
                "\t\t\"primary_location_full\": \"hellow.java\",\n" +
                "\t\t\"line\": 6,\n" +
                "\t\t\"analysis\": {\n" +
                "\t\t\t\"id\": \"list_node.issue_analysis_node.reviewed\",\n" +
                "\t\t\t\"type\": \"list_node\"\n" +
                "\t\t},\n" +
                "\t\t\"state\": {\n" +
                "\t\t\t\"id\": \"list_node.issue_state_node.new\",\n" +
                "\t\t\t\"type\": \"list_node\"\n" +
                "\t\t},\n" +
                "\t\t\"severity\": {\n" +
                "\t\t\t\"id\": \"list_node.severity.medium\",\n" +
                "\t\t\t\"type\": \"list_node\"\n" +
                "\t\t},\n" +
                "\t\t\"remote_id\": \"9793D70459978170AD8DF473412298C2\",\n" +
                "\t\t\"introduced_date\": \"2017-02-12T12:31:44.000+0000\",\n" +
                "\t\t\"external_link\": \"http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/8/issues/2742\",\n" +
                "\t\t\"extended_data\": {\n" +
                "\t\t\t\"issueName\": \"Poor Logging Practice: Use of a System Output Stream\",\n" +
                "\t\t\t\"likelihood\": \"1.0\",\n" +
                "\t\t\t\"impact\": \"1.0\",\n" +
                "\t\t\t\"confidence\": \"5.0\",\n" +
                "\t\t\t\"kingdom\": \"Encapsulation\",\n" +
                "\t\t\t\"removedDate\": null\n" +
                "\t\t},\n" +
                "\t\t\"tool_name\": \"external tool\"\n" +
                "\t}]\n" +
                "}";
    }

    private String getVulString() {
        return "{\n" +
                "  \"data\": [\n" +
                "    {\n" +
                "      \"severity\": {        \"type\": \"list_node\",        \"id\": \"list_node.severity.low\"      },\n" +
                "      \"package\": \"hp.com\",\n" +
                "      \"line\": 10,\n" +
                "      \"remote_id\": \"10341\",\n" +
                "      \"primary_location_full\": null,\n" +
                "      \"introduced_date\": \"2018-06-03T14:06:58Z\",\n" +
                "      \"owner_email\":\"daniel.shmaya@hpe.com\",\n" +
                "      \"state\": {        \"type\": \"list_node\",        \"id\": \"list_node.issue_state_node.closed\"      },\n" +
                "      \"tool_type\": {        \"type\": \"list_node\",        \"id\": \"list_node.securityTool.fod\"      },\n" +
                "      \"tool_name\": \"external tool\",\n" +
                "      \"external_link\":\"some url here\",\n" +
                "       \"analysis\": {        \"type\": \"list_node\",        \"id\": \"list_node.issue_analysis_node.maybe_an_issue\"      },\n" +
                "       \"extended_data\" : {\"key\":\"value\",\"key\":\"value\"},\n" +
                "      \"category\": \"category\"\n" +
                "    }, {\n" +
                "      \"severity\": {        \"type\": \"list_node\",        \"id\": \"list_node.severity.high\"      },\n" +
                "      \"package\": \"hp.com.com\",\n" +
                "      \"line\": 11,\n" +
                "      \"remote_id\": \"10321\",\n" +
                "      \"primary_location_full\": \"entities-factory.html\",\n" +
                "      \"introduced_date\": \"2018-06-03T14:06:58Z\",\n" +
                "      \"owner_email\":\"sa@nga\",\n" +
                "      \"state\": {        \"type\": \"list_node\",        \"id\": \"list_node.issue_state_node.new\"      },\n" +
                "      \"tool_type\": {        \"type\": \"list_node\",        \"id\": \"list_node.securityTool.fod\"      },\n" +
                "      \"tool_name\": \"external too 2\",\n" +
                "      \"external_link\":\"some url here 2\",\n" +
                "       \"analysis\": {        \"type\": \"list_node\",        \"id\": \"list_node.issue_analysis_node.reviewed\"      },\n" +
                "       \"extended_data\" : {\"key1\":\"value1\",\"key2\":\"value2\"},\n" +
                "      \"category\": \"category 2\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }
}

