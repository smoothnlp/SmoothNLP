package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.SEntity;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.basic.UtilFns;

import com.smoothnlp.nlp.model.crfpp.ModelImpl;
import com.smoothnlp.nlp.model.crfpp.Tagger;

//import com.smoothnlp.nlp.model.crfagu.ModelImpl;
//import com.smoothnlp.nlp.model.crfagu.Tagger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class NerCRFPP extends BaseEntityRecognizer{

    protected ModelImpl model;
    protected ISequenceTagger segment_pipeline;
    protected ISequenceTagger postag_pipeline;

    public NerCRFPP(){
        this.model = new ModelImpl();
        this.model.open(SmoothNLP.CRF_NER_MODEL,2,0,1.0);
    }

    private String buildFtrs(String... ftrs){
        return CRFModel.join("\t",ftrs);
    }

    public List<SEntity> process(String input){
        List<SToken> stokens = SmoothNLP.SEGMENT_PIPELINE.process(input);
        stokens = SmoothNLP.POSTAG_PIPELINE.process(stokens);
        return this.process(stokens);
    }

    public List<SEntity> process(List<SToken> stokens){

        Tagger tagger = this.model.createTagger();

        if (tagger==null){
            SmoothNLP.LOGGER.severe(String.format("CRF segment model is not properly read"));
        }

        if (stokens == null || stokens.size() == 0) {
            return new ArrayList<>();
        }

        for (SToken stoken: stokens){
            // 注意坑: crf_test 中默认将空格转化为空(""); 为了和crf模型效果保持一致, 这里采用了hardcode
            String ftr = this.buildFtrs(stoken.getToken().replaceAll("\\s",""),stoken.getPostag(),Integer.toString(stoken.token.length()));
            tagger.add(ftr);

        }
        tagger.parse();

        List<SEntity> ners = new LinkedList<>();
        List<SToken> temTokens = new LinkedList<>();
        List<Integer> temIndexes = new LinkedList<>();

        String prev_ytag = "O";

//        int charEnd = 0;
        int charCounter = 0;
        for (int i = 0; i<stokens.size();i++){

            int index = i+1; // <- stoken index

            int tagIndex = tagger.y(i);
//                if (tagIndex==0){  // 静止tagger 预测标点, 标点由强规则匹配
            double bestPorba = 0;

            for (int j = 0; j<7; j++){
//                System.out.println(tagger.yname(j)+":"+tagger.prob(i,j));
                if (tagger.prob(i,j)>bestPorba){
                    tagIndex = j;
                    bestPorba = tagger.prob(i,j);
                };
            }

            String ytag  = tagger.yname(tagIndex);

            if (ytag.contains("-")){
                ytag = ytag.split("-")[1];
            }

            if (!ytag.equals(prev_ytag) & !prev_ytag.equals("O")){

                SEntity entity = new SEntity(0,charCounter,temTokens,prev_ytag);
                entity.charStart = entity.charEnd - entity.text.length();

                entity.sTokenList = new HashMap<>();
                for (int j = 0;j< temIndexes.size();j+=1){
                    entity.sTokenList.put(temIndexes.get(j),temTokens.get(j));
                }

                ners.add(entity);
                temTokens = new LinkedList<>();
                temIndexes = new LinkedList<>();
            }

            if (!ytag.equals("O")){
                temTokens.add(stokens.get(i));
                temIndexes.add(index);
            }
            prev_ytag = ytag;
            charCounter += stokens.get(i).getToken().length();

            // 打印预测出的tag 和他们的概率, 用于debug
//                System.out.print(stokens.get(i).token);
//                System.out.print("\t");
//                System.out.print(ytag);
//                System.out.println("\t");
        }
        if (!temTokens.isEmpty()){
            SEntity entity = new SEntity(0,charCounter,temTokens,prev_ytag);
            ners.add(entity);
        }

        return ners;
    }

    public static void main(String[] args){
        IEntityRecognizer s = new NerCRFPP();
//        System.out.println(s.process("五十块钱买了两个冰淇淋还是挺便宜的"));
//        System.out.println(UtilFns.toJson(s.process("广汽集团上月利润达到5万,相比同期增长5%")));
//        System.out.println(UtilFns.toJson(s.process("广汽集团上月利润达到5万 相比同期增长5%")));
//        System.out.println(UtilFns.toJson(s.process("京东与格力开展战略合作丨家居要闻点评")));
//        System.out.println(UtilFns.toJson(s.process("京东与格力开展战略合作 家居要闻点评")));
//        System.out.println(UtilFns.toJson(s.process("SmoothNLP与腾讯科技达成战略合作")));
//        System.out.println(UtilFns.toJson(s.process("嗅问与腾讯科技达成战略合作")));
//        System.out.println(UtilFns.toJson(s.process("上海文磨与阿里巴巴达成战略合作")));
//        System.out.println(UtilFns.toJson(s.process("上海文磨获得有成创投的天使轮融资")));
//        System.out.println(UtilFns.toJson(s.process("嗅问知道上线一个月后日活突破3000万")));
//        System.out.println(UtilFns.toJson(s.process("中国石油在非洲开辟新油田")));
//
//        System.out.println(UtilFns.toJson(s.process("[期货]国际油价收市涨1.1% 美联储宣布推出QE4")));
//        System.out.println(UtilFns.toJson(s.process("新定价机制 有望6月推出")));
//
//        System.out.println(UtilFns.toJson(s.process("我国最大的服装行业展会明年起将移师上海 拓展商贸和服务功能")));
//        System.out.println(UtilFns.toJson(s.process("放大物流通道功能 开启对俄合作新篇章")));
        System.out.println(UtilFns.toJson(s.process("抖音")));
    }

}
