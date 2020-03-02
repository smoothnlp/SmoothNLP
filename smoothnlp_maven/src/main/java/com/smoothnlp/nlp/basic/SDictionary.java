package com.smoothnlp.nlp.basic;

import com.smoothnlp.nlp.SmoothNLP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

import com.smoothnlp.nlp.basic.IDictionary.MatchResult;

public class SDictionary implements IDictionary{
    private Map<String, List<String>> wordLibrary;
    private Map<String, Pattern> patterns = new HashMap<>();
    private List<String> activeLibraries = null;

    public SDictionary(Map<String,String> libraryMaps){
        this.wordLibrary = new HashMap<>();
        setDictionary(libraryMaps);
    }

    public void setDictionary(Map<String,String> libraryMaps){
        for (String label: libraryMaps.keySet()){
            String fileName = libraryMaps.get(label);
            List<String> wordList = new LinkedList<>();
            try {
                InputStream is = SmoothNLP.IOAdaptor.open(fileName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                while(reader.ready()) {
                    String word = reader.readLine();
                    word = word.replace("\n","");
                    wordList.add(word);
                }
                this.wordLibrary.put(label,wordList);
            }catch(IOException e){
                SmoothNLP.LOGGER.severe(e.getMessage());
            }
        }
        buildPatterns();
        resetActiveLibraries();
    }

//    public SDictionary(String[] args){
//        this.wordLibrary = new HashMap<>();
//        for (int i = 0; i<args.length;i=i+1){
//            String fileName = args[i];
//            String label = fileName.split(",")[0];
//            List<String> wordList = new LinkedList<>();
//            try {
//                InputStream is = SmoothNLP.IOAdaptor.open(fileName);
//                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//                while(reader.ready()) {
//                    String word = reader.readLine();
//                    wordList.add(word);
//                }
//                this.wordLibrary.put(label,wordList);
//            }catch(IOException e){
//                SmoothNLP.LOGGER.severe(e.getMessage());
//            }
//        }
//        buildPatterns();
//        resetActiveLibraries();
//    }

    public void buildPatterns(){
        for (String label: this.wordLibrary.keySet()){
            Pattern p = Pattern.compile(UtilFns.join("|",this.wordLibrary.get(label)));
            patterns.put(label,p);
        }
    }

    public void setActiveLibraries(List<String> libs){
        if (libs ==null){
            resetActiveLibraries();
        }else{
            this.activeLibraries = libs;
        }

    }

    public void resetActiveLibraries(){
        this.activeLibraries = new LinkedList<>();
        this.activeLibraries.addAll(this.patterns.keySet());
    }

    public List<MatchResult> find(String inputText){
        List<MatchResult> matches = new LinkedList<>();
        for (String label : this.activeLibraries){
            Pattern p = this.patterns.get(label);
            Matcher matcher =  p.matcher(inputText);
            while (matcher.find()) {
                matches.add(new MatchResult(matcher.start(), matcher.end(), label));
            }
        }
        return matches;
    }

    public List<MatchResult> find(String inputText, List<String> libraries){
        setActiveLibraries(libraries);
        List<MatchResult> matches = this.find(inputText);
        resetActiveLibraries();
        return matches;
    }



    public static void main(String[] args){
        Map<String, String> libraries = new HashMap<String, String>() {
            {
                put("datetime","test.txt");
            }
        };
        IDictionary dict = new SDictionary(SmoothNLP.regexLibraries);
        System.out.println(UtilFns.toJson(dict.find("深圳市太阳卡通策划设计有限公司")));
        System.out.println(UtilFns.toJson(dict.find("杭州阿里哈哈网络科技有限公司")));
    }

}
