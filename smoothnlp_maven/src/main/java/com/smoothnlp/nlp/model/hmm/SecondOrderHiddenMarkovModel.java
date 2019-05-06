package com.smoothnlp.nlp.model.hmm;

public class SecondOrderHiddenMarkovModel extends HiddenMarkovModel{
    /**
     * 状态转移概率矩阵
     */
    float [][] [] transition_probability2;

    private SecondOrderHiddenMarkovModel(float[]start_probability, float[][] transition_probability, float[][] emission_probability){
        super(start_probability, transition_probability, emission_probability);
    }
}
