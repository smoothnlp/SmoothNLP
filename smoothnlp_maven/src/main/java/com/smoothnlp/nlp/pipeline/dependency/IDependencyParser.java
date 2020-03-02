package com.smoothnlp.nlp.pipeline.dependency;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.SToken;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;

import java.io.InputStream;
import java.util.List;

public interface IDependencyParser {

    /**
     * For Now, the dependency interface are allowed to throw error, this may be updated later
     * @param input
     * @return
     * @throws Error
     */

    public DependencyRelationship[] parse(String input) throws XGBoostError;

    public DependencyRelationship[] parse(List<SToken> stokens) throws XGBoostError;

}
