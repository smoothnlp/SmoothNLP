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
        float[][] predictScores = this.edgeScoreModel.predict(dmatrix);
        float[] predictScoresFlatten = UtilFns.flatten2dFloatArray(predictScores);

        float[][] edgeScores = new float[cgraph.size()][cgraph.size()];
        for (int i =0; i<cgraph.size(); i++){
            for (int j = 0; j<cgraph.size(); j++){
                if (i!=j){  // 过滤一个token 自己依赖自己的情况
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
        System.out.println(UtilFns.toJson(dparser.parse("AI创业公司“一览群智”完成1.5亿元融资，经纬中国、策源资本投资")));
        System.out.println(UtilFns.toJson(dparser.parse("中共中央政治局召开会议,分析研究2019年经济工作")));
        System.out.println(UtilFns.toJson(dparser.parse("阿里与腾讯达成合作协议")));
        System.out.println(UtilFns.toJson(dparser.parse("文磨获得天使轮融资")));

    }

}
