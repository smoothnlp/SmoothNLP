package com.smoothnlp.nlp.learner;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.basic.UtilFns;

import java.util.List;

public abstract class TokenLearner implements ILearner{

    protected boolean fitted;

    public TokenLearner(){
        this.fitted = false;
    }

    public HashMap<String,String> encodeToken(String token){
        return new HashMap<String,String>();
    }

    public void fit(String inputText){};

    public void fit(String[] sentences){
        for (String sent: sentences){ this.fit(sent);}
    };

    public void fit(String inputCorpus, String sep){
        String[] sentences = inputCorpus.split(sep);
        this.fit(sentences);
    }

    public static String[] listSTokens2StringArray(List<SToken> tokens){
        String [] tokenArray = new String[tokens.size()];
        for (int i = 0; i<tokenArray.length; i++){
            tokenArray[i] = tokens.get(i).getToken();
        }
        return tokenArray;
    }

    protected void compute(){};

    public String transform(String inputText){
        if (!this.fitted){
            this.compute();
            this.fitted = true;
        }
        String[] inputTokens = listSTokens2StringArray(SmoothNLP.SEGMENT_PIPELINE.process(inputText));
        int charIndex = 0;
        ArrayList<HashMap<String,String>> resList = new ArrayList<HashMap<String, String>>();
        for (int i = 0; i< inputTokens.length; i++){
            HashMap<String,String> tokenRes = this.encodeToken(inputTokens[i]);
            tokenRes.put("token",inputTokens[i]);
            tokenRes.put("tokenIndex",Integer.toString(i));
            tokenRes.put("charStart",Integer.toString(charIndex));
            charIndex+=inputTokens[i].length();
            tokenRes.put("charEnd",Integer.toString(charIndex));
            resList.add(tokenRes);
        }
        return UtilFns.toJson(resList);
    }

    public String transform(String[] sentences){
        String[] transformed_res = new String[sentences.length];
        for (int i=0;i<sentences.length;i++){
            transformed_res[i] = transform(sentences[0]);
        }
        return UtilFns.toJson(transformed_res);
    }

    public static void main(String[] args){
        String[] tokens = listSTokens2StringArray(SmoothNLP.SEGMENT_PIPELINE.process("文本处理"));
        System.out.println(Arrays.toString(tokens));
    }

}
