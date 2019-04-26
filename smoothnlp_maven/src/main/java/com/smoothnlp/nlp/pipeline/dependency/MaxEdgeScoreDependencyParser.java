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

    public MaxEdgeScoreDependencyParser(){
        init(SmoothNLP.DP_EDGE_SCORE_XGBOOST);
    }

    public MaxEdgeScoreDependencyParser(String modelPath){
        init(modelPath);
    }

    private void init(String modelPath){
        this.edgeScoreModel = loadXgbModel(modelPath);
    }

    public List<DependencyRelationship> parse(String input) throws XGBoostError{
        List<SToken> stokens = SmoothNLP.POSTAG_PIPELINE.process(input);
        return parse(stokens);
    }

    public List<DependencyRelationship> parse(List<SToken> stokens) throws XGBoostError{
        CoNLLDependencyGraph cgraph = new CoNLLDependencyGraph(stokens);
        System.out.println(stokens);
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
                edgeScores[i][j] = predictScoresFlatten[i*cgraph.size()+j];
            }
        }
        cgraph.setEdgeScores(edgeScores);
        return cgraph.parseDependencyRelationships();
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
        System.out.println(dparser.parse("因为股票涨了, 所以我特别开心"));

    }

}
