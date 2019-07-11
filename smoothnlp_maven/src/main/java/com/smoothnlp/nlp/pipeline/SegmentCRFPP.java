package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.model.crfpp.*;

import com.smoothnlp.nlp.basic.*;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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

        // get stop/blank tags by matching keywords
        List<IDictionary.MatchResult> matchedRanges = SmoothNLP.DICTIONARIES.find(input,libraryNames);

        for (SDictionary.MatchResult match: matchedRanges){
            int start = match.start;
            int end = match.end;
            for (int j = start; j<end; j++){
                ytags[j] = BLANK_LABEL;
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
    }

}
