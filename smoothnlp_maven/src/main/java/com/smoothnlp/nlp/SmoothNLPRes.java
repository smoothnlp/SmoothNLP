package com.smoothnlp.nlp;

import com.smoothnlp.nlp.basic.UtilFns;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.pipeline.dependency.DependencyRelationship;

import java.util.*;

public class SmoothNLPRes {
    private class smoothNLPresult {
        public List<SToken> sTokensPOS;
        public List<DependencyRelationship> dependencyRelationshipList;
    }

    public String smoothProcess(String inputText) throws Exception {

        smoothNLPresult res = new smoothNLPresult();
        List<SToken> sTokensPOS = SmoothNLP.SEGMENT_PIPELINE.process(inputText);
        res.sTokensPOS = sTokensPOS;
        List<DependencyRelationship> dependencyRelationships=SmoothNLP.DEPENDENCY_PIPELINE.parse(inputText);
        res.dependencyRelationshipList = dependencyRelationships;
        return UtilFns.toJson(res);

    }

    public static void main(String[] args) throws Exception{
        String inputText = new String("我买了五斤苹果，总共10元");
        System.out.println(inputText);
        SmoothNLPRes s = new SmoothNLPRes();
        String result = s.smoothProcess(inputText);
        System.out.println(result);
    }
}
