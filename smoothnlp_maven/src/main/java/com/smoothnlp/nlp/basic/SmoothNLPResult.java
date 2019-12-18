package com.smoothnlp.nlp.basic;

import java.util.List;

import com.smoothnlp.nlp.basic.SEntity;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.pipeline.dependency.DependencyRelationship;

public class SmoothNLPResult {
    public List<SToken> tokens;
    public DependencyRelationship[] dependencyRelationships;
    public List<SEntity> entities;
    public String errMsg;

    public SmoothNLPResult() {}

    public SmoothNLPResult(String errMsg) {
        this.errMsg = errMsg;
    }
}