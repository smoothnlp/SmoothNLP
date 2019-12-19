package com.smoothnlp.nlp.model.crfagu;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/** 参考理解：https://blog.csdn.net/aws3217150/article/details/69212445
 *
 * Created by zhifac on 2017/3/25.
 */
public class CRFEncoderThread implements Callable<Integer> {
    public List<TaggerImpl> x; //Tagger数组，训练数据量
    public int start_i;  //该线程待处理的起始tagger的坐标
    public int wSize;
    public int threadNum;  // 总共有threadNum 来计算
    public int zeroone;
    public int err;
    public int size;
    public double obj;
    public double[] expected; //存放期望值

    public int wESize;
    public double[] expectedEmbedding;

    public CRFEncoderThread(int wsize) {
        if (wsize > 0) {
            this.wSize = wsize;
            expected = new double[wsize];
            Arrays.fill(expected, 0.0);
        }
    }


    public Integer call() {
        obj = 0.0;
        err = zeroone = 0;
        if (expected == null) {
            expected = new double[wSize];
        }
        Arrays.fill(expected, 0.0);
        for (int i = start_i; i < size; i = i + threadNum) { // 每个线程并行处理多个子句，且各自处理的句子不相同，size 为整体训练样本中句子的个数
            obj += x.get(i).gradient(expected); //用第i个tagger即句子的gradient进行计算；主要功能：构建无向图，调用前后向算法；计算期望
            int errorNum = x.get(i).eval();   //评估本次计算时，该句子中错误预测label的数量；
            x.get(i).clearNodes();
            err += errorNum;  //累计发生预测错误的序列label数量
            if (errorNum != 0) {
                ++zeroone;  // 该次序列预测中存在错误时累加一次；
            }
        }
        return err;
    }
}
