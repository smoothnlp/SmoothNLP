package com.smoothnlp.nlp.model.hmm;

import com.smoothnlp.nlp.utility.MathUtility;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public abstract class HiddenMarkovModel {
    /**
     * 初始状态概率向量
     *
     * 1.segment : s, e, m, b ,start_probability.length = 4;
     */
    float[] start_probability;

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

    protected void toLog(){
        if (start_probability == null || transition_probability == null || emission_probability == null) return ;
        for (int i = 0; i < start_probability.length; i++){
            start_probability[i] = (float) Math.log(start_probability[i]);
            for (int j = 0; j< start_probability.length; j++){
                transition_probability[i][j] = (float) Math.log(transition_probability[i][j]);
            }
            for (int j = 0; j<emission_probability.length;j++){
                emission_probability[i][j] = (float) Math.log(emission_probability[i][j]);
            }
        }
    }

    public void unlog(){
        for (int i = 0; i < start_probability.length; i++ ){
            start_probability[i] = (float)Math.exp(start_probability[i]);
        }
        for (int i = 0; i < emission_probability.length; i++){
            for (int j = 0; j <emission_probability[i].length; j++){
                emission_probability[i][j] = (float) Math.exp(emission_probability[i][j]);
            }
        }

        for (int i=0; i < transition_probability.length; i++){
            for(int j=0; j<transition_probability[i].length; j++){
                transition_probability[i][j] = (float) Math.exp(transition_probability[i][j]);
            }
        }
    }

    /**
     *  train
     * @param samples 数据集 int[i][j]  i = 0 为观测
     */
    public void train(Collection<int[][]> samples){


        if(samples.isEmpty()) return ;
        int max_state = 0;
        int max_obser = 0 ;

        for (int [][] sample:samples){
            if (sample.length != 2 || sample[0].length != sample[1].length) throw new IllegalArgumentException("非法样本");
            for (int o : sample[0])
                max_obser = Math.max(max_obser, o);
            for (int s : sample[1])
                max_state = Math.max(max_state, s);
        }

        estimateStartProbability(samples, max_state);
        estimateTransitionProbability(samples, max_state);
        estimateEmissionProbability(samples, max_state, max_obser);
        toLog();

    }

    /**
     * 频次向量归一化为概率分布
     *
     * @param freq
     */
    protected void normalize(float[] freq)
    {
        float sum = MathUtility.sum(freq);
        for (int i = 0; i < freq.length; i++)
            freq[i] /= sum;
    }

    /**
     * 估计初始状态概率向量
     * @param samples
     * @param max_state
     */

    protected void estimateStartProbability(Collection<int[][]> samples, int max_state){

        start_probability = new float[max_state + 1];

        for (int[][] sample :samples){
            int s  = sample[1][0];
            ++start_probability[s];
        }
        normalize(start_probability);
    }

    /**
     * 利用极大似然估计转移概率
     *
     * @param samples   训练样本集
     * @param max_state 状态的最大下标，等于N-1
     */
    protected void estimateTransitionProbability(Collection<int[][]> samples, int max_state)
    {
        transition_probability = new float[max_state + 1][max_state + 1];
        for (int[][] sample : samples)
        {
            int prev_s = sample[1][0];
            for (int i = 1; i < sample[0].length; i++)
            {
                int s = sample[1][i];
                ++transition_probability[prev_s][s];
                prev_s = s;
            }
        }
        for (int i = 0; i < transition_probability.length; i++)
            normalize(transition_probability[i]);
    }

    /**
     * 估计状态发射概率
     *
     * @param samples   训练样本集
     * @param max_state 状态的最大下标
     * @param max_obser 观测的最大下标
     */
    protected void estimateEmissionProbability(Collection<int[][]> samples, int max_state, int max_obser)
    {
        emission_probability = new float[max_state + 1][max_obser + 1];
        for (int[][] sample : samples)
        {
            for (int i = 0; i < sample[0].length; i++)
            {
                int o = sample[0][i];
                int s = sample[1][i];
                ++emission_probability[s][o];
            }
        }
        for (int i = 0; i < transition_probability.length; i++)
            normalize(emission_probability[i]);
    }

    /**
     * 预测 （维比特算法）
     * @param o 观测序列
     * @param s 预测状态序列（需预先分配内存）
     * @return 概率的对数， 可利用（float）Math.exp(maxScore) 还原
     */
    public abstract float predict(int[] o, int[] s);

}
