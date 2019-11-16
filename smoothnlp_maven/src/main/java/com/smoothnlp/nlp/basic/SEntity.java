package com.smoothnlp.nlp.basic;
import java.util.*;
import java.util.stream.Collectors;

public class SEntity implements Comparable<SEntity>{

    public int charStart;

    public int charEnd;

    public String text ;

    public String nerTag;

    /**
     * mapping from token index to SToken; Index START FROM 1
     */
    public Map<Integer, SToken> sTokenList ;

    public String normalizedEntityValue;

    public SEntity(){}

    public SEntity(int charStart, int charEnd, SToken token, String nerTag){
        this.charStart = charStart;
        this.charEnd = charEnd;
        this.text = token.getToken();
        this.nerTag = nerTag;
        this.normalizedEntityValue = text;
    }

    public SEntity(int charStart, int charEnd, List<SToken> tokens, String nerTag){
        this.charStart = charStart;
        this.charEnd = charEnd;

        String nerText = "";
        for (SToken t: tokens){
            nerText += t.getToken();
        }
        this.text = nerText;

        this.nerTag = nerTag;
        this.normalizedEntityValue = text;
    }

    public SEntity(int charStart, int charEnd, String token, String nerTag){
        this.charStart = charStart;
        this.charEnd = charEnd;
        this.text = token;
        this.nerTag = nerTag;
        this.normalizedEntityValue = text;
    }

    public String toString(){
        if (nerTag == null){
            return null;
        }else{
            // TO DO
            return this.text+"/"+this.nerTag;
        }
    }

    public String getText(){
        return this.sTokenList.entrySet()
        .stream()
        .sorted(((o1, o2) -> o1.getKey().compareTo(o2.getKey())))
        .map(e->e.getValue().token)
        .collect(Collectors.joining(""));
    }

    public int compareTo(SEntity target){
//        System.out.println(this.text+" compare entity:"+target.text);
        return -(this.charEnd-this.charStart) + (target.charEnd - target.charStart);
    }

}
