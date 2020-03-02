package com.smoothnlp.nlp;

import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;


import com.smoothnlp.nlp.basic.*;
import com.smoothnlp.nlp.io.*;
import com.smoothnlp.nlp.pipeline.*;
import com.smoothnlp.nlp.pipeline.dependency.CKYDependencyParser;
import com.smoothnlp.nlp.pipeline.dependency.DependencyRelationship;
import com.smoothnlp.nlp.pipeline.dependency.IDependencyParser;
import com.smoothnlp.nlp.pipeline.dependency.MaxEdgeScoreDependencyParser;

import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;

public class SmoothNLP{

    public SmoothNLP(){ }

    public static String NAME = "SmoothNLP";
    public static Logger LOGGER = Logger.getLogger("SmoothNLP");
    public static IIOAdapter IOAdaptor = new ResourceIOAdapter();

    // static libraries based on word dictionaries
    public static Map<String, String> dictLibraries = new HashMap<String, String>() {
        {
            put("GS", "financial_agencies.txt");
            put("FINANCE_METRIX", "financial_metrics.txt");
            put("METRIX_ACTION", "metric_action.txt");
            put("COMPANY_METRIX","organization_metrics.txt");
            put("COMMON","common_tokens.txt");
            put("COMMON_CHENGYU","common_chengyu.txt");
            put("COMMON_FINANCE","common_finance_tokens.txt");
        }
    };

    // static libraries based on regex patterns
    public static Map<String, String> regexLibraries = new HashMap<String, String>() {
        {
            put("RELATIVE_TIME","datetime_relative.txt");
            put("DATETIME","datetime.txt");
            put("COMPANY_REGISTR","company_registr.txt");
        }
    };

    public static IDictionary regexDict = new SDictionary(regexLibraries);


    public static IDictionary trieDict = new TrieDictionary(dictLibraries);

    // static Dictionary
    public static IDictionary DICTIONARIES = new MultiDictionary(new IDictionary[]{regexDict,trieDict});

    // static model files
    public static String CRF_SEGMENT_MODEL = "model/segment_ctb_4gram_f5_e4_B.bin";

//    public static String CRF_POSTAG_MODEL = "model/postag_3gram_B_f10_e5.bin";
//    public static String CRF_POSTAG_MODEL = "model/embed_3gram_f20_e5_dim300_NT_600.bin";
    public static String CRF_POSTAG_MODEL = "model/postag_all_embed_3gram_f20_e5_dim300_NT_600.bin";

    public static String CRF_NER_MODEL = "model/ner_4gram_B_200110.bin";
//    public static String CRF_NER_MODEL = "model/embed_ner_f10_e4.bin";


    public static String DP_EDGE_SCORE_XGBOOST = "model/dpedge_model_ftr169.bin";
    public static String DP_EDGE_TAG_XGBOOST = "model/dptag_model_ftr169.bin";

    public static int XGBoost_DP_Edge_Model_Predict_Tree_Limit = 64;  // 用于提升EdgeModel Predict时效率
    public static int XGBoost_DP_tag_Model_Predict_Tree_Limit = 32;   // 用于提升TagModel Predict时效率

    public static String WordEmbedding_MODEL = "embedding/vectors_dim32_win15.txt";

    // static Pipelines
    public static BaseSequenceTagger SEGMENT_PIPELINE = new SegmentCRFPP();

    public static PostagCRFPP POSTAG_PIPELINE = new PostagCRFPP();
//    public static IDependencyParser DEPENDENCY_PIPELINE = new MaxEdgeScoreDependencyParser();
    public static IDependencyParser DEPENDENCY_PIPELINE = new CKYDependencyParser();
    public static BaseEntityRecognizer NORMALIZED_NER = new NormalizedNER();
    public static BaseEntityRecognizer CRF_NER = new NerCRFPP();

    public static BaseEntityRecognizer REGEX_NER = new RegexNER(true);
    public static MultiNersPipeline NER_PIPELINE = new MultiNersPipeline(new BaseEntityRecognizer[]{NORMALIZED_NER,REGEX_NER,CRF_NER});
    public static WordEmbedding WORDEMBEDDING_PIPELINE = new WordEmbedding();
//    public static IEntityRecognizer STOKEN_NER = new RegexNER(new String[]{"STOPWORDS","stopwords.txt"},false);

    public static Pattern PUPattern = Pattern.compile("[！，。,;；？……]+"); // 不包括书名号,感叹号,小括号（）() 顿号、冒号：~@#￥% +—— & 空格 [\s]+| \[\] *丨
    public static String SegmentPUPattern ="[\\s]+|[+——！【】～__“”|，。/？、~@#￥%……&*（）()》《丨\\[\\]]{1}";
    public static Pattern NUMPattern = Pattern.compile("[点两双一二三四五六七八九零十〇\\d.%十百千万亿]{2,8}");


    public static synchronized SmoothNLPResult process(String inputText) throws XGBoostError {

        SmoothNLPResult res = new SmoothNLPResult();

        long start = System.currentTimeMillis();
        List<SToken> sTokensPOS = POSTAG_PIPELINE.process(inputText);
        res.tokens = sTokensPOS;
        long end = System.currentTimeMillis();
        System.out.println();
        System.out.print("token size: "+res.tokens.size()+"; ");

        System.out.print("segment+postag time: ");
        System.out.print(end-start+" | ");

        start = System.currentTimeMillis();
        res.entities = NER_PIPELINE.process(res.tokens);
        end = System.currentTimeMillis();
        System.out.print("ner time: ");
        System.out.print(end-start+" | ");

        start = System.currentTimeMillis();
        DependencyRelationship[] dependencyRelationships=DEPENDENCY_PIPELINE.parse(res.tokens);
        res.dependencyRelationships = dependencyRelationships;
        end = System.currentTimeMillis();
        System.out.print("dependency time: ");
        System.out.print(end-start+" | ");
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
//         System.out.println(UtilFns.toJson(SmoothNLP.process("腾讯和京东三季度营收分别是30亿与40亿")));
//         System.out.println(UtilFns.toJson(SmoothNLP.process("微信app日活达到1.3亿")));
//         System.out.println(UtilFns.toJson(SmoothNLP.process("华为作为手机制造企业代表，今年一季度生产手机842.55万台，产值达45.29亿元，同比增长3.8%；")));
//
////         System.out.println(UtilFns.toJson(DICTIONARIES.find("腾讯云在去年5月实现营收达到3亿元")));
//
////         System.out.println("test normalized ner: ");
//
//         System.out.println(UtilFns.toJson(SmoothNLP.process("玩手机")));
//
         System.out.println(UtilFns.toJson(SmoothNLP.process("大屏手机")));
//
         System.out.println(UtilFns.toJson(SmoothNLP.process("深圳厚屹照明有限公司坐落于深圳经济特区")));
         System.out.println(UtilFns.toJson(SmoothNLP.process("安徽(钰诚)控股集团、钰诚国际控股集团有限公司")));
         System.out.println(UtilFns.toJson(SmoothNLP.process("杭州(钰诚)控股集团、钰诚国际控股集团有限公司")));
         System.out.println(UtilFns.toJson(SmoothNLP.process("上海(钰诚)控股集团、钰诚国际控股集团有限公司")));
         System.out.println(UtilFns.toJson(SmoothNLP.process("扬州(钰诚)控股集团、钰诚国际控股集团有限公司")));

         System.out.println(UtilFns.toJson(SmoothNLP.process("杭州(阿里哈哈)网络科技有限公司")));
         System.out.println(UtilFns.toJson(SmoothNLP.process("杭州（网易）网络科技有限公司")));

         System.out.println(UtilFns.toJson(SmoothNLP.process("杭州阿里哈哈网络科技有限公司")));
         System.out.println(UtilFns.toJson(SmoothNLP.process("星晖新能源智能汽车生产基地是省重点发展项目之一，总投资超过200亿元，于2018年1月在黄冈产业园正式开工。")));

//         System.out.println(UtilFns.toJson(SmoothNLP.process("邯郸市通达机械制造有限公司建于一九八九年，位于河北永年高新技术工业园区，拥有固定资产1200万元，现有职工280名")));

         System.out.println(UtilFns.toJson(SmoothNLP.process("中国第一家股份制企业北京天桥百货股份有限公司成立；")));
       System.out.println(UtilFns.toJson(SmoothNLP.process("邯郸市通达机械制造有限公司拥有固定资产1200万元，现有职工280名，其中专业技术人员80名，高级工程师两名，年生产能力10000吨，产值8000万元")));




     }
}
