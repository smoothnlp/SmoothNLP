package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.basic.SDictionary;
import com.smoothnlp.nlp.basic.SEntity;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.UtilFns;

import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.InputStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexNER extends BaseEntityRecognizer {

    private HashMap<String,String> word2label;
    private Set<String> labelSet; // keep track of all labels, may change to string[] in later implementation

    /**
     * whether to use match implemented by Regex Matcher
     * if TRUE:
     *      regex.Matcher was used to implement
     * if FALSE:
     *      querrying input was first segmented to tokens, a entity is matched if it match exactly one token
     *      ex: if [国泰,君安] was segmented, and "国泰君安" was in the entity library, it WON'T BE MATCHED.
     *      MEANWHILE,  if TRUE, it won't have such problem.
     */
    private boolean useRegexMatch;

    public RegexNER(boolean useRegexMatch){
        this.useRegexMatch = useRegexMatch;
    }

    public RegexNER(List<String> libraryNames, boolean useRegexMatch){
        this.setActiveDictionaries(libraryNames);
        this.useRegexMatch = useRegexMatch;
    }

    public List<SEntity> process(List<SToken> sTokenList){
        List<SEntity> entityList = new ArrayList<SEntity>();
        int charCounter = 0;
        for (int i = 0; i<sTokenList.size(); i++){
            String token = sTokenList.get(i).getToken();

            List<SDictionary.MatchResult> matches = SmoothNLP.DICTIONARIES.find(token,this.libraryNames);
            if (matches.size()==1){
                SDictionary.MatchResult match = matches.get(0);
                if (match.end - match.start==token.length()){
                    entityList.add(new SEntity(charCounter,charCounter+token.length(),token,match.label));
                }
            }
            charCounter+=token.length();
        }
        return entityList;
    };

    public List<SEntity> process(String inputText){
        if (!this.useRegexMatch){
            return process(SmoothNLP.SEGMENT_PIPELINE.process(inputText));
        }else{
            List<SEntity> entityList = new ArrayList<SEntity>();
            List<SDictionary.MatchResult> matches = SmoothNLP.DICTIONARIES.find(inputText,this.libraryNames);
            for (SDictionary.MatchResult match : matches){
                SEntity entity = new SEntity(match.start,match.end,inputText.substring(match.start,match.end),match.label);
                entityList.add(entity);
            }
            return entityList;
        }
    };

    public static void main(String[] args) throws IOException{
        System.out.println("hello");
        RegexNER n = new RegexNER(true);
        List<SEntity> l = n.process("5月3日");
        System.out.println(l);
        System.out.println(n.process("2019年5月3日"));
        System.out.println(n.process("五月三日"));
        System.out.println(n.process("5月三日"));
        System.out.println(n.process("二零一九年"));
        System.out.println(n.process("二零一九年五月三日"));

    }

}
