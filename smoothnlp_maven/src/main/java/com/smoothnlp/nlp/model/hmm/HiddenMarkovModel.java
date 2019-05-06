package com.smoothnlp.nlp.model.hmm;

/**
 *
 */
public abstract class HiddenMarkovModel {
    /**
     * 初始状态概率向量
     */
    float [] start_probability;

    /**
     *  观测转移概率矩阵
     */
    float [][] emission_probability;

    /**
     * 状态转移概率矩阵
     */
    float [][] transition_probability;

    /**
     * 构造隐马模型
     *
     * @param start_probability      初始状态概率向量
     * @param transition_probability 状态转移概率矩阵
     * @param emission_probability   观测概率矩阵
     */
    public HiddenMarkovModel(float[] start_probability, float[][] transition_probability, float[][] emission_probability)
    {
        this.start_probability = start_probability;
        this.transition_probability = transition_probability;
        this.emission_probability = emission_probability;
    }

}
