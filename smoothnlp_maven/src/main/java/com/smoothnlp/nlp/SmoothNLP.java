package com.smoothnlp.nlp;

import java.util.List;
import java.util.logging.Logger;

import com.smoothnlp.nlp.basic.SEntity;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.basic.UtilFns;
import com.smoothnlp.nlp.io.*;
import com.smoothnlp.nlp.pipeline.ISequenceTagger;
import com.smoothnlp.nlp.pipeline.*;
import com.smoothnlp.nlp.pipeline.dependency.DependencyRelationship;
import com.smoothnlp.nlp.pipeline.dependency.IDependencyParser;
import com.smoothnlp.nlp.pipeline.dependency.MaxEdgeScoreDependencyParser;
import com.sun.org.apache.regexp.internal.RE;

public class SmoothNLP{

    public static String NAME = "SmoothNLP";

    public static Logger LOGGER = Logger.getLogger("SmoothNLP");

    public static IIOAdapter IOAdaptor = new ResourceIOAdapter();

    // static libraries
    public static String[] libraries = new String[]{"financial_agencies.txt","financial_metrix.txt","financial_metrix_action.txt"};

    // static model files
    public static String CRF_SEGMENT_MODEL = "segment_crfpp.bin";
    public static String CRF_POSTAG_MODEL = "postag_crfpp.bin";
    public static String DP_EDGE_SCORE_XGBOOST = "DP_Edge_Score_XgbModel.bin";

    // static Pipelines
    public static ISequenceTagger SEGMENT_PIPELINE = new SegmentCRFPP();
    public static ISequenceTagger POSTAG_PIPELINE = new PostagCRFPP();
    public static IDependencyParser DEPENDENCY_PIPELINE = new MaxEdgeScoreDependencyParser();
    public static IEntityRecognizer NORMALIZED_NER = new NormalizedNER();
    public static IEntityRecognizer REGEX_NER = new RegexNER(new String[]{"financial_agencies","financial_agencies.txt",
                                                                            "financial_metrix","financial_metrix.txt",
                                                                            "metrix_action","financial_metrix_action.txt"},true);
//    public static IEntityRecognizer STOKEN_NER = new RegexNER(new String[]{"STOPWORDS","stopwords.txt"},false);


    // simple static class for storing results
    private static class SmoothNLPresult {
        public List<SToken> tokens;
        public List<DependencyRelationship> dependencyRelationships;
        public List<SEntity> entities;
    }

    public static String process(String inputText) throws Exception{
        SmoothNLPresult res = new SmoothNLPresult();
        List<SToken> sTokensPOS = POSTAG_PIPELINE.process(inputText);
        res.tokens = sTokensPOS;
        List<DependencyRelationship> dependencyRelationships=DEPENDENCY_PIPELINE.parse(inputText);
        res.dependencyRelationships = dependencyRelationships;
        List<SEntity> normalizedEntities = NORMALIZED_NER.process(sTokensPOS);
        res.entities = normalizedEntities;
        res.entities.addAll(REGEX_NER.process(inputText));
//        res.entities.addAll(STOKEN_NER.process(sTokensPOS));
        return UtilFns.toJson(res);
    }

    public static void main(String[] args) throws Exception{
        System.out.println(process("纳斯达克100指数跌1%。纳指跌0.89%，标普500指数跌0.78%，道指跌约250点。"));
        System.out.println(process("我买了十斤水果"));
        System.out.println(process("国泰君安的估值去年上涨了百分之五十"));

    }

}
