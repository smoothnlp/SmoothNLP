package com.smoothnlp.nlp.basic;
import java.util.*;
import java.util.stream.Collectors;

public class SEntity {

    public int charStart;

    public int charEnd;

    public String text ;

    public String nerTag;

    public Map<Integer, SToken> sTokenList;

    public String normalizedEntityTag;

    public String toString(){
        if (nerTag == null){
            return null;
        }else{
            // TO DO
            return "";
        }
    }

    public String getText(){
        return this.sTokenList.entrySet()
        .stream()
        .sorted()
        .map(e->e.getValue().token)
        .collect(Collectors.joining(""));
    }

}
