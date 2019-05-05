package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.basic.SDictionarySupported;
import com.smoothnlp.nlp.basic.SEntity;
import com.smoothnlp.nlp.basic.SToken;

import java.util.*;

public interface IEntityRecognizer {
    public List<SEntity> process(List<SToken> sTokenList);
    public List<SEntity> process(String inputText);
}

