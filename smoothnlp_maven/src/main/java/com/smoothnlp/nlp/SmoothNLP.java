package com.smoothnlp.nlp;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;


import com.fasterxml.jackson.databind.ser.Serializers;
import com.smoothnlp.nlp.basic.*;
import com.smoothnlp.nlp.io.*;
import com.smoothnlp.nlp.pipeline.ISequenceTagger;
import com.smoothnlp.nlp.pipeline.*;
import com.smoothnlp.nlp.pipeline.dependency.DependencyRelationship;
import com.smoothnlp.nlp.pipeline.dependency.IDependencyParser;
import com.smoothnlp.nlp.pipeline.dependency.MaxEdgeScoreDependencyParser;

public class SmoothNLP{

    public SmoothNLP(){ }

    public static String NAME = "SmoothNLP";

    public static Logger LOGGER = Logger.getLogger("SmoothNLP");

    public static IIOAdapter IOAdaptor = new ResourceIOAdapter();

    // static libraries
    public static Map<String, String> libraries = new HashMap<String, String>() {
        {
            put("financial_agency", "financial_agencies.txt");
            put("financial_metric", "financial_metrics.txt");
            put("metrix_action", "metric_action.txt");
            put("organization_metric","organization_metrics.txt");
        }
    };

    // static Dictionary
    public static SDictionary DICTIONARIES = new SDictionary(libraries);

    // static model files
    public static String CRF_SEGMENT_MODEL = "segment_crfpp.bin";
    public static String CRF_POSTAG_MODEL = "postag_crfpp.bin";
    public static String DP_EDGE_SCORE_XGBOOST = "DP_Edge_Score_XgbModel.bin";

    // static Pipelines
    public static BaseSequenceTagger SEGMENT_PIPELINE = new SegmentCRFPP();
    public static BaseSequenceTagger POSTAG_PIPELINE = new PostagCRFPP();
    public static IDependencyParser DEPENDENCY_PIPELINE = new MaxEdgeScoreDependencyParser();
    public static BaseEntityRecognizer NORMALIZED_NER = new NormalizedNER();
    public static BaseEntityRecognizer REGEX_NER = new RegexNER(true);
//    public static IEntityRecognizer STOKEN_NER = new RegexNER(new String[]{"STOPWORDS","stopwords.txt"},false);



    public static SmoothNLPResult process(String inputText) throws Exception{
        SmoothNLPResult res = new SmoothNLPResult();
        List<SToken> sTokensPOS = POSTAG_PIPELINE.process(inputText);
        res.tokens = sTokensPOS;
        List<DependencyRelationship> dependencyRelationships=DEPENDENCY_PIPELINE.parse(inputText);
        res.dependencyRelationships = dependencyRelationships;
        List<SEntity> normalizedEntities = NORMALIZED_NER.process(sTokensPOS);
        res.entities = normalizedEntities;
        res.entities.addAll(REGEX_NER.process(inputText));
//        res.entities.addAll(STOKEN_NER.process(sTokensPOS));
        // return UtilFns.toJson(res);
        return res;
    }

     public static void main(String[] args) throws Exception{
         System.out.println(process("纳斯达克100指数跌1%。纳指跌0.89%，标普500指数跌0.78%，道指跌约250点。"));
         System.out.println(UtilFns.toJson(process("广汽集团一季度营收27.78亿").entities));
         System.out.println(UtilFns.toJson(process("广汽集团一季度营收上涨30%").entities));
         System.out.println(process("国泰君安的估值去年上涨了百分之五十"));

     }

}
