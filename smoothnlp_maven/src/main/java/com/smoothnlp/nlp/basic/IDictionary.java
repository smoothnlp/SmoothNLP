package com.smoothnlp.nlp.basic;

import java.util.List;
public interface IDictionary {

    public List<MatchResult> find(String inputText);
    public List<MatchResult> find(String inputText, List<String> libraries);

    public class MatchResult{
        public int start;
        public int end;
        public String label;

        public MatchResult(int start, int end, String label){
            this.start = start;
            this.end = end;
            this.label = label;
        }

    }

}
