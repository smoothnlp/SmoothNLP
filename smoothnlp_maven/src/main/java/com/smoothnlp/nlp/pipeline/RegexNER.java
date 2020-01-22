package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.basic.SDictionary;
import com.smoothnlp.nlp.basic.SEntity;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.UtilFns;

import java.io.IOException;
import java.util.*;
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

        List<String> StringTokens = new LinkedList<>();
        for (int i = 0; i<sTokenList.size(); i++){
            StringTokens.add(sTokenList.get(i).getToken());
        }
        String sentence = UtilFns.join("",StringTokens);

        List<SEntity> hardEntities = hardMatch(sentence);
        if (hardEntities.size()<1){return hardEntities;}  // 先通过强匹配, 如果没有entity, 则直接返回空list
        else{ // 处理如果强匹配找到entity的情况
            List<SEntity> entityList = new LinkedList<>();
            for (SEntity entity : hardEntities){

                if (entity.nerTag.length()<6){
                    continue;
                }

                if (entity.nerTag.substring(0,6).equals("COMMON")){
                    continue;
                }

                int charCounter = 0;
                boolean startChecker=false, endChecker=false;
                Map<Integer, SToken> tokenMap = new HashMap<>();
                int tokenIndex = 1; // notice token index starts from 1
                for (SToken token : sTokenList){
                    if (entity.charStart == charCounter){
                        startChecker=true;
                    }
                    charCounter+=token.token.length();
                    if (entity.charEnd == charCounter){
                        endChecker = true;
                    }
                    if (startChecker){
                        tokenMap.put(tokenIndex,token);
                    }
                    if (startChecker==true & endChecker==true){
                        entity.sTokenList = tokenMap;
                        entityList.add(entity);
                        break;
                    }
                    tokenIndex++;
                }
            }
            return entityList;
        }

    };

    public List<SEntity> process(String inputText){
        if (!this.useRegexMatch){
            return process(SmoothNLP.SEGMENT_PIPELINE.process(inputText));
        }else{
            return hardMatch(inputText);
        }
    };

    public List<SEntity> hardMatch(String inputText){
        List<SEntity> entityList = new LinkedList<>();
        List<SDictionary.MatchResult> matches = SmoothNLP.DICTIONARIES.find(inputText,this.libraryNames);
        for (SDictionary.MatchResult match : matches){
            SEntity entity = new SEntity(match.start,match.end,inputText.substring(match.start,match.end),match.label);
            entityList.add(entity);
        }
        return entityList;
    }

    public static void main(String[] args) throws IOException{
        System.out.println("hello");
        RegexNER n = new RegexNER(false);
        List<SEntity> l = n.process("5月3日");
        System.out.println(l);
        System.out.println(n.process("2019年5月3日"));
        System.out.println(n.process("五月三日"));
        System.out.println(n.process("5月三日"));
        System.out.println(n.process("二零一九年"));
        System.out.println(n.process("90元,二零一九年五月三日"));
        System.out.println(UtilFns.toJson(n.process("90元,二零一九年五月三日")));
    }

}
