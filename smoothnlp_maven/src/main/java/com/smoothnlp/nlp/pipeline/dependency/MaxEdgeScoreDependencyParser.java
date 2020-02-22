package com.smoothnlp.nlp.pipeline.dependency;

import com.smoothnlp.nlp.basic.CoNLLToken;
import com.smoothnlp.nlp.basic.SToken;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.UtilFns;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class MaxEdgeScoreDependencyParser implements IDependencyParser{

    private Booster edgeScoreModel;
    private Booster edgeTagModel;

    public MaxEdgeScoreDependencyParser(){
        init(SmoothNLP.DP_EDGE_SCORE_XGBOOST, SmoothNLP.DP_EDGE_TAG_XGBOOST);
    }

    public MaxEdgeScoreDependencyParser(String edgeScoreModel, String edgeTagModel){
        init(edgeScoreModel,edgeTagModel);
    }

    private void init(String edgeScoreModel, String edgeTagModel){
        this.edgeScoreModel = loadXgbModel(edgeScoreModel);
        this.edgeTagModel = loadXgbModel(edgeTagModel);
    }

    public DependencyRelationship[] parse(String input) throws XGBoostError{
        List<SToken> stokens = SmoothNLP.POSTAG_PIPELINE.process(input);
        return parse(stokens);
    }

    public DependencyRelationship[] parse(List<SToken> stokens) throws XGBoostError{
        CoNLLDependencyGraph cgraph = new CoNLLDependencyGraph(stokens);
        // build ftrs
        Float[][] pairFtrs = cgraph.buildAllFtrs();
        float[] flattenPairFtrs = UtilFns.flatten2dFloatArray(pairFtrs);


        int numRecords = pairFtrs.length;
        int numFtrs = pairFtrs[0].length;
        DMatrix dmatrix = new DMatrix(flattenPairFtrs,numRecords,numFtrs);

        float[][] predictScores = this.edgeScoreModel.predict(dmatrix,false,SmoothNLP.XGBoost_DP_Edge_Model_Predict_Tree_Limit);  // 调节treeLimit , 优化时间

        float[] predictScoresFlatten = UtilFns.flatten2dFloatArray(predictScores);
        float[][] edgeScores = new float[cgraph.size()][cgraph.size()];
        for (int i =0; i<cgraph.size(); i++){
            for (int j = 0; j<cgraph.size(); j++){
                if (i!=j){  // 过滤一个token 自己依赖自己的情况
                    // todo: 待评估
                    edgeScores[i][j] = predictScoresFlatten[i*cgraph.size()+j];
                }
            }
        }
        cgraph.setEdgeScores(edgeScores);

        return cgraph.parseDependencyRelationships(this.edgeTagModel);
    }


    public static Booster loadXgbModel(String modelAddr) {

        try{
            InputStream modelIS = SmoothNLP.IOAdaptor.open(modelAddr);
            Booster booster = XGBoost.loadModel(modelIS);
            return booster;
        }catch(Exception e){
            // add proper warnings later
            System.out.println(e);
            return null;
        }
    }

    public static void main(String[] args) throws XGBoostError{
        MaxEdgeScoreDependencyParser dparser = new MaxEdgeScoreDependencyParser();

//        long start = System.currentTimeMillis();
//        dparser.parse("邯郸市通达机械制造有限公司拥有固定资产1200万元，现有职工280名，其中专业技术人员80名，高级工程师两名，年生产能力10000吨，产值8000万元");
//        long end = System.currentTimeMillis();
//        System.out.print("dependency time: ");
//        System.out.println(end-start);
//
//        start = System.currentTimeMillis();
//        dparser.parse("邯郸市通达机械制造有限公司拥有固定资产1200万元，现有职工280名，其中专业技术人员80名，高级工程师两名，年生产能力10000吨，产值8000万元");
//        end = System.currentTimeMillis();
//        System.out.print("dependency time: ");
//        System.out.println(end-start);
        String text = "在面对用户的搜索产品不断丰富的同时，百度还创新性地推出了基于搜索的营销推广服务，并成为最受企业青睐的互联网营销推广平台。";
//        String text = "呼吸道飞沫传染是新型冠状病毒的主要传播途径";
        long start = System.currentTimeMillis();
        for (DependencyRelationship e : dparser.parse(text)) {
            System.out.println(e);
        }
        long end = System.currentTimeMillis();
        System.out.print("dependency time: ");
        System.out.println(end-start);
//        System.out.println(UtilFns.toJson(dparser.parse("阿里巴巴是以曾担任英语教师的马云为首的18人于1999年在浙江杭州创立的公司 ")));
//        System.out.println(UtilFns.toJson(dparser.parse("邯郸市通达机械制造有限公司拥有固定资产1200万元，现有职工280名，其中专业技术人员80名，高级工程师两名，年生产能力10000吨，产值8000万元")));
////        System.out.println(UtilFns.toJson(dparser.parse("中共中央政治局召开会议,分析研究2019年经济工作")));
//        System.out.println(UtilFns.toJson(dparser.parse("阿里与腾讯达成合作协议")));
//        System.out.println(UtilFns.toJson(dparser.parse("阿里巴巴在英属开曼群岛注册成立")));
//        System.out.println(UtilFns.toJson(dparser.parse("在面对用户的搜索产品不断丰富的同时，百度还创新性地推出了基于搜索的营销推广服务，并成为最受企业青睐的互联网营销推广平台。")));
//        System.out.println(UtilFns.toJson(dparser.parse("湖北华一专用汽车有限公司是随州专用汽车制造销售公司")));
//        System.out.println(UtilFns.toJson(dparser.parse("公开资料显示，香雪制药2016年通过股权受让的方式取得湖北天济55%股权。")));
//        System.out.println(UtilFns.toJson(dparser.parse("星晖新能源智能汽车生产基地是省重点发展项目之一，总投资超过200亿元，于2018年1月在黄冈产业园正式开工。")));

    }

}
