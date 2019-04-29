package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.basic.SEntity;
import com.smoothnlp.nlp.basic.SToken;

import java.util.*;

public interface iEntityClassifier {
    public List<SEntity> entityClassify(List<SToken> sTokenList);
}
