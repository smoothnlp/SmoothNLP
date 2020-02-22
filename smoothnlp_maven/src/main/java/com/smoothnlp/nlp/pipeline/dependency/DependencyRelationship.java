package com.smoothnlp.nlp.pipeline.dependency;

import com.smoothnlp.nlp.basic.SToken;

public class DependencyRelationship {

    public String relationship;
    private transient SToken dependent,target;
    public int dependentIndex,targetIndex;
    public float _edge_score,_tag_score;

    public static String UnknownRelationship = "UNKNOWN";

    public DependencyRelationship(int dependentIndex, int targetIndex){
        this.dependentIndex = dependentIndex;
        this.targetIndex = targetIndex;
        this.relationship = UnknownRelationship;
    }

    public DependencyRelationship(int dependentIndex, int targetIndex, SToken dependent, SToken target){
        this.dependentIndex = dependentIndex;
        this.targetIndex = targetIndex;
        this.relationship = UnknownRelationship;
        this.dependent = dependent;
        this.target = target;
    }

    public DependencyRelationship(int dependentIndex, int targetIndex, SToken dependent, SToken target, String relationship){
        this.dependentIndex = dependentIndex;
        this.targetIndex = targetIndex;
        this.relationship = relationship;
        this.dependent = dependent;
        this.target = target;
    }


    public String toString(){
        if (this.dependent==null | this.target ==null){
            return String.valueOf(this.dependentIndex) + String.format("  --(%s)-->  ",this.relationship)+String.valueOf(this.targetIndex);
        }else{
            return this.dependent.getToken() + String.format(" (%s) --(%s)--> (%s) ",this.dependentIndex,this.relationship,this.targetIndex)+this.target.getToken() + " "+this._edge_score+" - "+this._tag_score;
        }
    }

}
