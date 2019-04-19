package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.model.crfpp.*;

import com.smoothnlp.nlp.basic.*;


import java.util.ArrayList;
import java.util.List;

public class SegmentCRFPP implements SequenceTagger{

    protected ModelImpl model;
    private static String STOP_LABEL = "S";
    private static String BLANK_LABEL = "B";

    public SegmentCRFPP(){
        this.model = new ModelImpl();
        this.model.open(SmoothNLP.CRF_SEGMENT_MODEL,0,0,1.0);
    }

    public List<SToken> process(String input){
        Tagger tagger = this.model.createTagger();
        if (tagger==null){
            SmoothNLP.LOGGER.severe(String.format("CRF segment model is not properly read"));
        }
        if (input == null || input.length() == 0) {
            return new ArrayList<SToken>();
        } else {
            char[] chars = input.toCharArray();
            for (char c: chars) {
                System.out.println(c);
                tagger.add(c + "");
            }

            tagger.parse();
            StringBuilder temToken = new StringBuilder();
            List<SToken> resTokens = new ArrayList<SToken>();
            for (int i = 0; i < tagger.size(); i++){
                String ytag = tagger.yname(tagger.y(i));
                temToken.append(chars[i]);
                if (ytag.equals(STOP_LABEL)){
                    resTokens.add(new SToken(temToken.toString()));
                    temToken = new StringBuilder();
                }
            }
            return resTokens;
        }
    }

    public static void main(String[] args){
        SegmentCRFPP s = new SegmentCRFPP();
        System.out.println(s.process("你好,我在soho上班"));
    }

}
