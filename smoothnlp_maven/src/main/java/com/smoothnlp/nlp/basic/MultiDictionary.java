package com.smoothnlp.nlp.basic;

import java.util.LinkedList;
import java.util.List;

public class MultiDictionary implements IDictionary {

    private IDictionary[] dictionaries;
    public MultiDictionary(IDictionary[] dictionaries){
        this.dictionaries = dictionaries;
    }

    public List<MatchResult> find(String inputText){
        List<MatchResult> resList = new LinkedList<>();
        for (IDictionary dict: this.dictionaries){
            resList.addAll(dict.find(inputText));
        }
        return resList;
    }

    public List<MatchResult> find(String inputText, List<String> libraries){
        List<MatchResult> resList = new LinkedList<>();
        for (IDictionary dict: this.dictionaries){
            resList.addAll(dict.find(inputText,libraries));
        }
        return resList;
    }

}
