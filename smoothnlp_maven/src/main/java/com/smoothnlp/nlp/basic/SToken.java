package com.smoothnlp.nlp.basic;

public class SToken {

    public String token;
    public String postag;

    public SToken(String token){
        this.token = token;
        this.postag = null;
    }

    public SToken(String token, String postag){
        this.token = token;
        this.postag = postag;
    }

    public String toString(){
        return this.token;
    }

}
