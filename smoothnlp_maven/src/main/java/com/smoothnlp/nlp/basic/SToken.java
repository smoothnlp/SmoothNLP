package com.smoothnlp.nlp.basic;

public class SToken {

    public String token;
    public String postag;
    public float tagproba;

    public SToken(String token){
        this.token = token;
        this.postag = null;
    }

    public SToken(String token, String postag){
        this.token = token;
        this.postag = postag;
    }

    public String toString(){
        if (postag == null){
            return this.token;
        }else{
            return this.token+"/"+this.postag+"("+tagproba+")";
        }

    }

    public String getToken(){ return this.token; }

    public void setPostag(String postag) { this.postag = postag; }

    public String getPostag(){return this.postag;}

    public void setTagproba(double tagproba){this.tagproba = (float) tagproba;}

    public float getTagproba(){return this.tagproba;}

}
