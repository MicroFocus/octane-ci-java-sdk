//package com.hp.octane.integrations.services.vulnerabilities.fod.dto;
//
//import com.hp.mqm.analytics.devops.insights.entities.PipelineEntity;
//import com.hp.mqm.analytics.devops.insights.entities.PipelineEntityToken;
//import com.hp.mqm.analytics.devops.insights.entities.PipelineRunEntityToken;
//import com.hp.mqm.analytics.devops.insights.services.liveview.EntitiesReadUtils;
//import com.hp.mqm.analytics.devops.insights.services.liveview.FOD.POJOs.VulnerabilityAllData;
//import com.hp.mqm.analytics.devops.insights.services.liveview.FOD.Services.FODVulnerabilityService;
//import com.hp.mqm.analytics.devops.insights.services.liveview.FortifyCTP;
//import com.hp.mqm.app.entities.IssueEntity;
//import com.hp.mqm.bl.platform.BlPlatformApi;
//import com.hp.mqm.bl.platform.ReadResult;
//import com.hp.mqm.bl.platform.ReadService;
//import com.hp.mqm.ps.entitymodel.EntityModelApi;
//import com.hp.mqm.ps.entitymodel.PlatformFeatureFieldNames;
//import com.hp.mqm.ps.entitymodel.query.builder.ConditionBuilder;
//import com.hp.mqm.ps.entitymodel.query.builder.QueryBuilder;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * Created by hijaziy on 6/28/2018.
// */
//public class IssueAllData{
//
//    private static final Logger logger = LoggerFactory.getLogger(IssueAllData.class);
//
//    public IssueAllData(){
//
//    }
//    public IssueAllData(VulnerabilityAllData vulnerabilityAllData){
//        if(vulnerabilityAllData != null) {
//            if (vulnerabilityAllData.details != null) {
//                summary = vulnerabilityAllData.details.summary;
//                explanation = vulnerabilityAllData.details.explanation;
//            }
//            if(vulnerabilityAllData.recommendations != null) {
//                recommendations = vulnerabilityAllData.recommendations.recommendations;
//                tips = vulnerabilityAllData.recommendations.tips;
//            }
//        }
//    }
//    public String summary;
//    public String explanation;
//    public String recommendations;
//    public String tips;
//
//    public static IssueAllData createEmpty() {
//        IssueAllData issueAllData = new IssueAllData();
//        issueAllData.summary = "";
//        issueAllData.explanation = "";
//        issueAllData.recommendations = "";
//        issueAllData.tips = "";
//        return issueAllData;
//    }
//    public static IssueAllData getAllDataOfIssue(Long issueId){
//        if(issueId == null){
//            return IssueAllData.createEmpty();
//        }
//        IssueEntity issueEnt = EntitiesReadUtils.getIssueEntity(issueId);
//        Long fodReleaselOfPpln = EntitiesReadUtils.getFODReleaselOfPpln(getPipelineByIssue(issueId).getId());
//        VulnerabilityAllData vulnerabilityDetails = FODVulnerabilityService.getSingleVulnAlldata(fodReleaselOfPpln, issueEnt.getRemoteId());
//        return new IssueAllData(vulnerabilityDetails);
//
//    }
//
//    private static PipelineEntity getPipelineByIssue(Long issueId) {
//        ReadService PplReadService = BlPlatformApi.getBlServiceFactory().createService(ReadService.class, PipelineEntityToken.class);
//        QueryBuilder qb = EntityModelApi.getQueryBuilder();
//        ConditionBuilder cb = EntityModelApi.getConditionBuilder();
//        final ReadResult<PipelineEntity> result = PplReadService.read(qb
//                .select(PlatformFeatureFieldNames.ID)
//                .from(PipelineEntityToken.class)
//                .where(cb.cross(PipelineEntityToken.PIPELINE_RUN,
//                        cb.cross(PipelineRunEntityToken.ISSUE, cb.eq(PlatformFeatureFieldNames.ID, issueId))))
//                .build());
//        if(result.size()!=1){
//            logger.error("expected to get exectly 1 pipeline while actulay found : "+result.size());
//            return null;
//        }
//        return result.get(0);
//    }
//
//}