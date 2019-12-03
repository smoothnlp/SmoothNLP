package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.*;
import com.smoothnlp.nlp.model.crfpp.ModelImpl;
import com.smoothnlp.nlp.model.crfpp.Tagger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.smoothnlp.nlp.basic.IDictionary.MatchResult;

public class PostagCRFPP extends CRFModel{
    protected ModelImpl model;
    protected ISequenceTagger segment_pipeline;
    private List<String> libraryNames = null;
    private Pattern pupattern,numpattern;
//    private Map<String,String> tokenLibrary;
    private IDictionary tokenLibrary,datetimeLibrary;


    public PostagCRFPP(){
        this.model = new ModelImpl();
        this.model.open(SmoothNLP.CRF_POSTAG_MODEL,0,0,1.0);
        this.pupattern  = Pattern.compile("[\\s]+|[+——！，。？、~@#￥%……&*（）()》《丨\\[\\]]+");
        this.numpattern = Pattern.compile("[点两双一二三四五六七八九零十〇\\d|.|%|个|十|百|千|万|亿]+");

        this.datetimeLibrary = new SDictionary(new HashMap<String, String>() {
            {
                put("DTA", "datetime.txt");
                put("DTR", "datetime_relative.txt");
            }
        });

        this.tokenLibrary = new TrieDictionary(new HashMap<String, String>() {
            {
                put("CTY", "country.txt");
                put("LOC", "location.txt");
            }
        });
    }

    public PostagCRFPP(ISequenceTagger segment_pipeline){
        this.segment_pipeline = segment_pipeline;
    }

    public void setActiveDictionaries(List<String> libraryNames){
        this.libraryNames = libraryNames;
    }

    public void setSegment_pipeline(ISequenceTagger segment_pipeline) {
        this.segment_pipeline = segment_pipeline;
    }

    public List<SToken> process(String input){
        if (segment_pipeline==null){
            segment_pipeline = new SegmentCRFPP();
        }
        List<SToken> stokens = segment_pipeline.process(input);
        return process(stokens);
    };

    public List<SToken> process(List<SToken> stokens){
        Tagger tagger = this.model.createTagger();
        if (tagger==null){
            SmoothNLP.LOGGER.severe(String.format("CRF segment model is not properly read"));
        }
        if (stokens == null || stokens.size() == 0) {
            return new ArrayList<SToken>();
        }else{
            for (SToken stoken: stokens){
                String ftr = super.buildFtrs(stoken.getToken());
                tagger.add(ftr);
            }
            tagger.parse();
            for (int i=0; i<stokens.size();i++){

                String ytag = tagger.yname(tagger.y(i));  // predict的t

                // 打印预测出的tag 和他们的概率, 用于debug
//                System.out.print(stokens.get(i).token);
//                System.out.print("\t");
//                System.out.print(ytag);
//                System.out.print("\t");
//                System.out.println(tagger.prob(i));

                // 对于字符串为"空格" 或者其他中文标点, 如"丨"的token, 强行改写postag = "PU"
                Matcher pumatcher = this.pupattern.matcher(stokens.get(i).token);
                while (pumatcher.find()){
                    if (pumatcher.end() - pumatcher.start() == stokens.get(i).token.length()) {
                        ytag = "PU";
                    }
                }

                Matcher numMatcher = this.numpattern.matcher(stokens.get(i).token);
                while (numMatcher.find()){
                    if (numMatcher.end() - numMatcher.start() == stokens.get(i).token.length()){
                        ytag = "CD";
                    }
                }

                stokens.get(i).setPostag(ytag);

                List<MatchResult> res = this.tokenLibrary.find(stokens.get(i).token);
                for (MatchResult m: res){
                    if (m.end-m.start == stokens.get(i).token.length() ) {  // 检验是否是完整匹配
                        stokens.get(i).setPostag(m.label);
                    }
                }

                res = this.datetimeLibrary.find(stokens.get(i).token);
                for (MatchResult m: res){
                    if (m.end-m.start == stokens.get(i).token.length() ) {
                        stokens.get(i).setPostag(m.label);
                    }
                }

            }
        }
        return stokens;
    };

    public static void main(String[] args){
        ISequenceTagger s = new PostagCRFPP();
        System.out.println(s.process("五十块钱买了两个冰淇淋还是挺便宜的"));
        System.out.println(UtilFns.toJson(s.process("广汽集团上月利润达到5万,相比同期增长5%")));
        System.out.println(UtilFns.toJson(s.process("广汽集团上月利润达到5万 相比同期增长5%")));
        System.out.println(UtilFns.toJson(s.process("京东与格力开展战略合作丨家居要闻点评")));
        System.out.println(UtilFns.toJson(s.process("中国消费者协会")));
        System.out.println(UtilFns.toJson(s.process("上海市政府")));
        System.out.println(UtilFns.toJson(s.process("2015年windows10份额不到10%")));
        System.out.println(UtilFns.toJson(s.process("傅帆担任中国太保党委书记")));
        System.out.println(UtilFns.toJson(s.process("300亿元的全面战略合作协议")));
        System.out.println(UtilFns.toJson(s.process("[亚太]日经指数周四收高0.19%")));
        System.out.println(UtilFns.toJson(s.process("江山控股(00295)拟11.66元出售10个太阳能项目")));
    }

}
