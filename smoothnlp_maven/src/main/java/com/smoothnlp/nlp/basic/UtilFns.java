package com.smoothnlp.nlp.basic;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smoothnlp.nlp.SmoothNLP;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;

import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilFns {

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

    public static String join(String delimeter, Collection<String> contents){
        if (contents.size()<=0){return "";}
        else{
            StringBuilder sb = new StringBuilder();
            for (Object s: contents){
                sb.append(s.toString());
                sb.append(delimeter);
            }
            sb.setLength(sb.length()-delimeter.length());
            return sb.toString();
        }
    }

    public static String join(String delimeter, Object[] contents){
        if (contents.length<=0){return "";}
        else{
            StringBuilder sb = new StringBuilder();
            for (int i =0; i < contents.length; i++){
                sb.append(contents[i].toString());
                sb.append(delimeter);
            }
            sb.setLength(sb.length()-1);
            return sb.toString();
        }
    }

    public static float[] flatten2dFloatArray(Float[][] f2darray){
        float output[] = new float[f2darray.length*f2darray[0].length];
        int counter = 0;
        for (Float[] farray:f2darray){
            for (Float f: farray){
                output[counter]=(f != null ? f : Float.NaN);
                counter+=1;
            }
        }
        return output;
    }

    public static float[] flatten2dFloatArray(float[][] f2darray){
        float output[] = new float[f2darray.length*f2darray[0].length];
        int counter = 0;
        for (float[] farray:f2darray){
            for (float f: farray){
                output[counter]=f;
                counter+=1;
            }
        }
        return output;
    }

    public static String toJson(Object o){
        GsonBuilder gb = new GsonBuilder();
        gb = gb.serializeSpecialFloatingPointValues();
        gb = gb.serializeNulls();
        Gson gson = gb.create();
        return gson.toJson(o);
    }

    public static List<String> split2sentences(String corpus){
        return split2sentences(corpus, "[。，;]|[!?！？;]+");
    }

    public static List<String> split2sentences(String corpus, String splitsStr){
        List<String> sentences = new LinkedList<>();
        Pattern seg_patterns = Pattern.compile(splitsStr);
        Matcher matcher = seg_patterns.matcher(corpus);
        int indexer = 0;
        while (matcher.find()){
            sentences.add(corpus.substring(indexer,matcher.end()));
            indexer = matcher.end();
        }
        if (indexer ==0){
            sentences.add(corpus);
        }else{
            if (indexer<corpus.length()){
                sentences.add(corpus.substring(indexer+1));
            }
        }
        return sentences;
    }

    public static void main(String[] args){
        System.out.println(split2sentences("广汽集团上季度营收27.78亿; 广汽集团上月利润达到5千万"));
    }

}
