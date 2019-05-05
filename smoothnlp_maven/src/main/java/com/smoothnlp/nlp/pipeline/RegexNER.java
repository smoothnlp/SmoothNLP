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
    private Pattern patterns;

    public RegexNER(boolean useRegexMatch){
        this.useRegexMatch = useRegexMatch;
//        this.labelSet = new HashSet<String>();
//        this.word2label = new HashMap<>();
//        for (int i = 0; i<args.length;i=i+2){
//            String label = args[i];
//            this.labelSet.add(label);
//            String fileName = args[i+1];
//            try {
//                InputStream is = SmoothNLP.IOAdaptor.open(fileName);
//                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//                while(reader.ready()) {
//                    String word = reader.readLine();
//                    word2label.put(word,label);
//                }
//            }catch(IOException e){
//                SmoothNLP.LOGGER.severe(e.getMessage());
//            }
//        }
//        if (this.useRegexMatch){
//            this.patterns = Pattern.compile(UtilFns.join("|",word2label.keySet()));
//        }
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
        List<SEntity> l = n.process("万科是一家房地产企业,国泰君安是一家资本公司; 标普500指数上涨5个点");
        System.out.println(l);
    }

}
