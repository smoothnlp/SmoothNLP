package com.smoothnlp.nlp.pipeline;
import com.smoothnlp.nlp.basic.SToken;
import java.util.List;
public interface ISequenceTagger {
    public List<SToken> process(String input);
    public List<SToken> process(List<SToken> tokens);
}
