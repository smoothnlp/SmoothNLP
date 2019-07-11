package com.smoothnlp.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;


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

    // static libraries based on word dictionaries
    public static Map<String, String> dictLibraries = new HashMap<String, String>() {
        {
            put("COMPANY_NAME", "financial_agencies.txt");
            put("FINANCE_METRIX", "financial_metrics.txt");
            put("METRIX_ACTION", "metric_action.txt");
            put("COMPANY_METRIX","organization_metrics.txt");
        }
    };

    // static libraries based on regex patterns
    public static Map<String, String> regexLibraries = new HashMap<String, String>() {
        {
            put("RELATIVE_TIME","datetime_relative.txt");
            put("DATETIME","datetime.txt");
        }
    };

    public static IDictionary regexDict = new SDictionary(regexLibraries);
    public static IDictionary trieDict = new TrieDictionary(dictLibraries);

    // static Dictionary
    public static IDictionary DICTIONARIES = new MultiDictionary(new IDictionary[]{regexDict,trieDict});

    // static model files
    public static String CRF_SEGMENT_MODEL = "model/ctb_3gram_segment_f2_c1.5.bin";
    public static String CRF_POSTAG_MODEL = "model/ctb_3gram_postag_f5_c1.bin";
    public static String DP_EDGE_SCORE_XGBOOST = "model/dpmodel_500.bin";
    public static String WordEmbedding_MODEL = "embedding/wordembedding.txt";

    // static Pipelines
    public static BaseSequenceTagger SEGMENT_PIPELINE = new SegmentCRFPP();
    public static BaseSequenceTagger POSTAG_PIPELINE = new PostagCRFPP();
    public static IDependencyParser DEPENDENCY_PIPELINE = new MaxEdgeScoreDependencyParser();
    public static BaseEntityRecognizer NORMALIZED_NER = new NormalizedNER();
    public static BaseEntityRecognizer REGEX_NER = new RegexNER(true);
    public static MultiNersPipeline NER_PIPELINE = new MultiNersPipeline(new BaseEntityRecognizer[]{NORMALIZED_NER,REGEX_NER});
    public static WordEmbedding WORDEMBEDDING_PIPELINE = new WordEmbedding();
//    public static IEntityRecognizer STOKEN_NER = new RegexNER(new String[]{"STOPWORDS","stopwords.txt"},false);



    public static SmoothNLPResult process(String inputText) throws Exception{
        SmoothNLPResult res = new SmoothNLPResult();
        List<SToken> sTokensPOS = POSTAG_PIPELINE.process(inputText);
        res.tokens = sTokensPOS;
        List<DependencyRelationship> dependencyRelationships=DEPENDENCY_PIPELINE.parse(inputText);
        res.dependencyRelationships = dependencyRelationships;
        res.entities = NER_PIPELINE.process(inputText);
        return res;
    }


    public static String segment(String inputText){
        List<SToken> res= POSTAG_PIPELINE.process(inputText);
        List<String> segList = new ArrayList<>();
        for(SToken st : res){
            segList.add(st.token);
        }
        return UtilFns.toJson(segList);
    }

    public static String postag(String inputText){
        return UtilFns.toJson(POSTAG_PIPELINE.process(inputText));
    }

    public static String ner(String inputText){
        return UtilFns.toJson(NER_PIPELINE.process(inputText));
    }

     public static void main(String[] args) throws Exception{
         //System.out.println(process("纳斯达克100指数跌1%。纳指跌0.89%，标普500指数跌0.78%，道指跌约250点。"));
         //System.out.println(UtilFns.toJson(process("广汽集团一季度营收27.78亿").entities));
         //System.out.println(UtilFns.toJson(process("广汽集团一季度营收上涨30%").entities));
         //System.out.println(process("国泰君安的估值去年上涨了百分之五十"));
         //System.out.println(UtilFns.toJson(process("董秘工资哪家高？万科董秘年薪超八百万笑傲董秘圈 ｜ 资色").entities));
         //System.out.println(UtilFns.toJson(process("广汽集团1月利润达到5").entities));
//         System.out.println(segment("2019年三月"));
//         System.out.println(postag("5月5日"));
//         System.out.println(postag("百分点这家科技公司, 在过去的30年中, 营收上涨了30个百分点"));
//         System.out.println(postag("华为去年生产值不少于50%"));
//         System.out.println("--------");
//         System.out.println(segment("腾讯前三季度云服务的总收入超过60亿元，而前三季度，腾讯以支付及相关服务和云服务为主的其他收入累计金额为538亿元左右，由此推算，三季度腾讯的支付及相关业务累计营收额约为478亿元。"));
//         System.out.println(ner("腾讯前三季度云服务的总收入超过60亿元，而前三季度，腾讯以支付及相关服务和云服务为主的其他收入累计金额为538亿元左右，由此推算，三季度腾讯的支付及相关业务累计营收额约为478亿元。"));
//         postag("纳斯达克100指数跌1%。纳指跌0.89%，标普500指数跌0.78%，道指跌约250点。");

//         System.out.println(UtilFns.toJson(WORDEMBEDDING_PIPELINE.process("的")));
//
//         System.out.println(UtilFns.toJson(SmoothNLP.process("腾讯和京东三季度营收分别是30亿与40亿").dependencyRelationships));
//         System.out.println(UtilFns.toJson(SmoothNLP.process("华为作为手机制造企业代表，今年一季度生产手机842.55万台，产值达45.29亿元，同比增长3.8%；").dependencyRelationships));


         System.out.println(UtilFns.toJson(DICTIONARIES.find("腾讯云在去年5月实现营收达到3亿元")));

         System.out.println("test normalized ner: ");

         System.out.println(UtilFns.toJson(SmoothNLP.process("生动态,第四范式获三大国有银行共同战略投资")));

     }
}
