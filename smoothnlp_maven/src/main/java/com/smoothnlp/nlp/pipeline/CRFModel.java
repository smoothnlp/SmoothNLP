package com.smoothnlp.nlp.pipeline;

import com.sun.deploy.util.StringUtils;

import java.util.Arrays;

public abstract class CRFModel {
    public String buildFtrs(char c, String[] ftrs){
        return buildFtrs(String.valueOf(c),ftrs);
    }

    public String buildFtrs(String token, String[] ftrs){
        return token+"\t"+ StringUtils.join(Arrays.asList(ftrs),"\t");
    }

    public String buildFtrs(char c){return String.valueOf(c);}

}
