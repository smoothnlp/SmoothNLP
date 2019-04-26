package com.smoothnlp.nlp.pipeline.dependency;

import com.smoothnlp.nlp.basic.SToken;

public class DependencyRelationship {

    public String relationship;
    public SToken dependent,target;
    public int dependentIndex,targetIndex;

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

    public String toString(){
        if (this.dependent==null | this.target ==null){
            return String.valueOf(this.dependentIndex) + String.format("  --(%s)-->  ",this.relationship)+String.valueOf(this.targetIndex);
        }else{
            return this.dependent.getToken() + String.format("  --(%s)-->  ",this.relationship)+this.target.getToken();
        }
    }

}
