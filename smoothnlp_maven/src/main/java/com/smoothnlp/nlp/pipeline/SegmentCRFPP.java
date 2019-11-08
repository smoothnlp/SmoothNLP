package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.model.crfpp.*;

import com.smoothnlp.nlp.basic.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SegmentCRFPP extends CRFModel{

    protected ModelImpl model;
    private static String STOP_LABEL = "S";
    private static String BLANK_LABEL = "B";
    private List<String> libraryNames = null;


    public SegmentCRFPP(){
        this.model = new ModelImpl();
        this.model.open(SmoothNLP.CRF_SEGMENT_MODEL,0,0,1.0);

    }

    public void setActiveDictionaries(List<String> libraryNames){
        this.libraryNames = libraryNames;
    }

    public List<SToken> process(List<SToken> tokens){return tokens;}

    public List<SToken> process(String input){

        Tagger tagger = this.model.createTagger();
        if (tagger==null){
            SmoothNLP.LOGGER.severe(String.format("CRF segment model is not properly read"));
        }
        if (input == null || input.length() == 0) {
            return new ArrayList<SToken>();
        }
        char[] chars = input.toCharArray();
        for (char c: chars) {
            String ftrs = super.buildFtrs(c);  // Build ftrs for crf needed sequence, in this case, only each char is needed
            tagger.add(ftrs);
        }
        tagger.parse();
        StringBuilder temToken = new StringBuilder();
        List<SToken> resTokens = new ArrayList<SToken>();

        String[] ytags = new String[tagger.size()];

        // get stop/blank tags from crf model
        for (int i = 0; i < tagger.size(); i++){
            ytags[i] = tagger.yname(tagger.y(i));
        }

        // 添加逻辑:
        // 1. 遇到空格切开
        // 2. 中英文相连切开, (特殊情况会被字典Overwrite, 如: "A轮"出现在字典中)
        // 3. 其他字符(标点等)和英文等没有分开
        Pattern pattern  = Pattern.compile("[a-zA-Z]+|[ ]+|[0-9|,|.|%|个|十|百|千|万|亿]+");
        Matcher matcher =  pattern.matcher(input);
        while (matcher.find()) {
            for (int i = matcher.start();i< matcher.end()-1;i++){
                ytags[i]= BLANK_LABEL;
            }
            ytags[matcher.end()-1] = STOP_LABEL;
        }


        List<IDictionary.MatchResult> matchedRanges = SmoothNLP.DICTIONARIES.find(input,libraryNames);
        Collections.sort(matchedRanges);  // 按照match 到token的长度进行排序
        // 按照词典的匹配进行切词
        for (SDictionary.MatchResult match: matchedRanges){

            int start = match.start;
            int end = match.end;
            for (int j = start; j<end; j++){
                ytags[j] = BLANK_LABEL;
            }
            if (start > 0){
                ytags[start-1] = STOP_LABEL;
            }
            ytags[end-1] = STOP_LABEL;
        }

        // build tokens
        for (int i =0; i<tagger.size();i++){
            temToken.append(chars[i]);
            if (ytags[i].equals(STOP_LABEL)) {
                resTokens.add(new SToken(temToken.toString()));
                temToken = new StringBuilder();
            }
        }
        return resTokens;
    }

    public static void main(String[] args){
        SegmentCRFPP s = new SegmentCRFPP();
        System.out.println(s.process("腾讯云是一家云服务提供商"));
        System.out.println(s.process("Gucci母公司开云集团2017财年销售额破150亿欧元"));
        System.out.println(s.process("广发证券:震荡中加大对港股配置 关注超跌低估值板块  ##\"关注\"前的空格被保留"));
        System.out.println(s.process("维达国际(03331.HK)第三季度折旧摊销前溢利大增82.7%至6.45亿..."));
        System.out.println(s.process("文磨完成A轮融资"));
    }

}
