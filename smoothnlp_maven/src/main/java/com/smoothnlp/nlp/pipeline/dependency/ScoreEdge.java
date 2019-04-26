package com.smoothnlp.nlp.pipeline.dependency;

public class ScoreEdge implements Comparable<ScoreEdge>{
    public int source,target;
    public float score;

    public ScoreEdge(int source, int target, float score){
        this.source= source;
        this.target = target;
        this.score = score;
    }

    public int compareTo(ScoreEdge se){
        float diff =  this.score - se.score;
        if (diff>0){return -1;} else if (diff==0){return 0;} else {return 1;}
    }


}
