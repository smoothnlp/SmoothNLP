package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.basic.SToken;
import java.util.List;
public interface SequenceTagger{
    public List<SToken> process(String input);
}
