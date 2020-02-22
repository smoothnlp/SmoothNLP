package com.smoothnlp.nlp.model.crfagu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhifac on 2017/3/18.
 */
public class Encoder {
    public static int MODEL_VERSION = 100;
    public enum Algorithm { CRF_L2, CRF_L1, MIRA }

    public Encoder() {}

    /**
     *
     * @param templFile 模板文件
     * @param trainFile 训练文件
     * @param modelFile 模型文件
     * @param embeddingFile embedding 预训练结果文件
     * @param textModelFile 是否将模型文件存储为文本格式
     * @param maxitr 最大循环迭代次数
     * @param freq use features that occuer no less than INT， 限制feature至少出现的次数，一般default为1 ，即出现就会作为特征放入；
     * @param eta
     * @param C
     * @param threadNum  //支持线程调度，CrfLearn 调用时是根据当前机器存在的空闲线程数进行提供；测试设置为1
     * @param shrinkingSize
     * @param algo
     * @return
     */
    public boolean learn(String templFile, String trainFile, String modelFile, String embeddingFile, boolean textModelFile,
                         int maxitr, int freq, double eta, double C, int threadNum, int shrinkingSize,
                         Algorithm algo,
                         String embeddingDefMode) {
        if (eta <= 0) {
            System.err.println("eta must be > 0.0");
            return false;
        }
        if (C < 0.0) {
            System.err.println("C must be >= 0.0");
            return false;
        }
        if (shrinkingSize < 1) {
            System.err.println("shrinkingSize must be >= 1");
            return false;
        }
        if (threadNum <= 0) {
            System.err.println("thread must be  > 0");
            return false;
        }
        EncoderFeatureIndex featureIndex = new EncoderFeatureIndex(threadNum);// featureIndex 中存储所有的特征；
        List<TaggerImpl> x = new ArrayList<TaggerImpl>();

        if (embeddingFile != null){ //如果支持embedding, 则打开该文件并加载数据；
            if(!featureIndex.open(templFile,trainFile,embeddingFile, embeddingDefMode)){
                System.err.println("Fail to open " + templFile + " " + trainFile+ " " + embeddingFile);
            }
        }else if (!featureIndex.open(templFile, trainFile)) {  // 打开模板文件和训练文件，并根据tempFile初始化了templs 和labels
            System.err.println("Fail to open " + templFile + " " + trainFile);
        }

        File file = new File(trainFile);
        if (!file.exists()) {
            System.err.println("train file " + trainFile + " does not exist.");
            return false;
        }
        BufferedReader br = null;
        try {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "UTF-8");
            br = new BufferedReader(isr);
            int lineNo = 0;
            while (true) {
                TaggerImpl tagger = new TaggerImpl(TaggerImpl.Mode.LEARN);
                tagger.open(featureIndex);
                TaggerImpl.ReadStatus status = tagger.read(br);
                // 读取训练文件的每个sentence，训练文件中每个sentence 以空白行分割，
                // 以词性标注为例，每行代表一个词，每列是词的特征；多个词（多行）代表一个句子，句子与句子之间用空白行进行分割；
                //该函数读取一个句子，并对于 tagger 中的数据结构 进行初始化
                if (status == TaggerImpl.ReadStatus.ERROR) {
                    System.err.println("error when reading " + trainFile);
                    return false;
                }
                if (!tagger.empty()) {
                    if (!tagger.shrink()) { // 单个sentence读取结束， 为该句做buildFeatures
                        System.err.println("fail to build feature index ");
                        return false;
                    }
                    tagger.setThread_id_(lineNo % threadNum);  //tagger 对应分配至的thread_id，即lineNo分配至的线程ID
                    x.add(tagger);
                } else if (status == TaggerImpl.ReadStatus.EOF) {
                    break;
                } else {
                    continue;
                }
                if (++lineNo % 100 == 0) {
                    System.out.print(lineNo + ".. ");
                }
            }
            br.close();
            System.out.println();
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        featureIndex.shrink(freq, x); // 不起作用

        //初始化所有特征的参数值为0
        double[] alpha = new double[featureIndex.size()];
        Arrays.fill(alpha, 0.0);
        featureIndex.setAlpha_(alpha);

        //初始化embeddingvector 部分的特征值
        double[] alphaEmbedding = new double[featureIndex.sizeEmbedding()];
        Arrays.fill(alphaEmbedding, 0.0);
        featureIndex.setAlphaEmbedding_(alphaEmbedding);

        System.out.println("Number of sentences: " + x.size());
        System.out.println("Number of features:  " + featureIndex.size());
        System.out.println("Number of embedding vector size:" + featureIndex.getEmbeddingVectorSize());
        System.out.println("NUmber of embedding template vector size: " + featureIndex.sizeEmbedding());
        System.out.println("Number of y:" + featureIndex.ysize() );
        System.out.println("Number of thread(s): " + threadNum);
        System.out.println("Freq:                " + freq);
        System.out.println("eta:                 " + eta);
        System.out.println("C:                   " + C);
        System.out.println("shrinking size:      " + shrinkingSize);

        switch (algo) {
            case CRF_L1:
                if (!runCRF(x, featureIndex, alpha, alphaEmbedding, maxitr, C, eta, shrinkingSize, threadNum, true, modelFile)) {
                    System.err.println("CRF_L1 execute error");
                    return false;
                }
                break;
            case CRF_L2:
                if (!runCRF(x, featureIndex, alpha, alphaEmbedding, maxitr, C, eta, shrinkingSize, threadNum, false, modelFile)) {
                    System.err.println("CRF_L2 execute error");
                    return false;
                }
                break;
            case MIRA:
                if (!runMIRA(x, featureIndex, alpha, maxitr, C, eta, shrinkingSize, threadNum)) {
                    System.err.println("MIRA execute error");
                    return false;
                }
                break;
            default:
                break;
        }

        if (!featureIndex.save(modelFile, textModelFile)) {
            System.err.println("Failed to save model");
        }
        System.out.println("Done!");
        return true;
    }

    /**
     *
     * @param x
     * @param featureIndex
     * @param alpha
     * @param maxItr
     * @param C
     * @param eta
     * @param shrinkingSize
     * @param threadNum
     * @param orthant
     * @return
     */
    private boolean runCRF(List<TaggerImpl> x,
                           EncoderFeatureIndex featureIndex,
                           double[] alpha,
                           double[] alphaEmbedding,
                           int maxItr,
                           double C,
                           double eta,
                           int shrinkingSize,
                           int threadNum,
                           boolean orthant,
                           String modelFile) {
        double oldObj = 1e+37;
        int converge = 0;
        LbfgsOptimizer lbfgs = new LbfgsOptimizer();


        // 支持多线程操作的过程；
        //List<CRFEncoderThread> threads = new ArrayList<CRFEncoderThread>();

        List<CRFEmbeddingEncoderThread> threads = new ArrayList<CRFEmbeddingEncoderThread>();

        for (int i = 0; i < threadNum; i++) {
            //CRFEncoderThread thread = new CRFEncoderThread(alpha.length);
            CRFEmbeddingEncoderThread thread = new CRFEmbeddingEncoderThread(alpha.length, alphaEmbedding.length);
            thread.start_i = i;
            thread.size = x.size();
            thread.threadNum = threadNum;
            thread.x = x;
            threads.add(thread);
        }

        int all = 0;
        for (int i = 0; i < x.size(); i++) {
            all += x.get(i).size();
        }

        // 多线程执行计算gradient；迭代次数为maxItr;
        ExecutorService executor = Executors.newFixedThreadPool(threadNum);
        for (int itr = 0; itr < maxItr; itr++) {

            if (itr%100==0 & itr>0){
                featureIndex.save(modelFile.replace(".bin","_")+itr+".bin",false);
            }

            featureIndex.clear();

            try {
                executor.invokeAll(threads); //多线程计算,调用 CRFEncoderThread的call()
            } catch(Exception e) {
                e.printStackTrace();
                return false;
            }

            // 以上结束多线程调用，开始计算单次循环中的数值求和过程；


            for (int i = 1; i < threadNum; i++) {
                threads.get(0).obj += threads.get(i).obj;
                threads.get(0).err += threads.get(i).err;
                threads.get(0).zeroone += threads.get(i).zeroone;
            }
            // 计算期望
            for (int i = 1; i < threadNum; i++) {
                for (int k = 0; k < featureIndex.size(); k++) {
                    threads.get(0).expected[k] += threads.get(i).expected[k];
                }

                for (int k = 0; k < featureIndex.sizeEmbedding(); k++){
                    threads.get(0).expectedEmbedding[k] += threads.get(i).expectedEmbedding[k];
                }
            }

            int numNonZero = 0;  // 统计参数中的非零项；
            int numNonZeroEmbedding = 0; // embedding 参数中的非零项；

            if (orthant) {  // L1 根据L1或者L2正则化，更新似然函数值；
                for (int k = 0; k < featureIndex.size(); k++) {
                    threads.get(0).obj += Math.abs(alpha[k] / C);
                    if (alpha[k] != 0.0) {
                        numNonZero++;
                    }
                }
                for (int k = 0; k <featureIndex.sizeEmbedding(); k++){
                    threads.get(0).obj += Math.abs(alphaEmbedding[k] / C);
                    if(alphaEmbedding[k] != 0.0){
                        numNonZero++;
                        numNonZeroEmbedding++;
                    }
                }
            } else { //L2
                numNonZero = featureIndex.size() + featureIndex.sizeEmbedding();
                numNonZeroEmbedding = featureIndex.sizeEmbedding();
                for (int k = 0; k < featureIndex.size(); k++) {
                    threads.get(0).obj += (alpha[k] * alpha[k] / (2.0 * C));
                    threads.get(0).expected[k] += alpha[k] / C;
                }
                for (int k = 0; k < featureIndex.sizeEmbedding();k++){
                    threads.get(0).obj += (alphaEmbedding[k] * alphaEmbedding[k] /(2.0 * C));
                    threads.get(0).expectedEmbedding[k] += alphaEmbedding[k] /C;
                }

            }
            for (int i = 1; i < threadNum; i++) {
                // try to free some memory
                threads.get(i).expected = null;
                threads.get(i).expectedEmbedding = null;
            }

            double diff = (itr == 0 ? 1.0 : Math.abs(oldObj - threads.get(0).obj) / oldObj);
            StringBuilder b = new StringBuilder();
            b.append("iter=").append(itr);
            b.append(" terr=").append(1.0 * threads.get(0).err / all);
            b.append(" serr=").append(1.0 * threads.get(0).zeroone / x.size());
            b.append(" act=").append(numNonZero);
            b.append(" actEmbedding=").append(numNonZeroEmbedding);
            b.append(" obj=").append(threads.get(0).obj);
            b.append(" diff=").append(diff);
            System.out.println(b.toString());

            oldObj = threads.get(0).obj;

            if (diff < eta) {
                converge++;
            } else {
                converge = 0;
            }

            if (itr > maxItr || converge == 3) {
                break;
            }

            // 传入似然函数值和梯度等参数，调用LBFGS 算法；
            //int ret = lbfgs.optimize(featureIndex.size(), alpha, threads.get(0).obj, threads.get(0).expected, orthant, C);

            // 将alpha 和 alphaEmbedding 拼接，featureIndex.size() + featureIndex.sizeEmbedding(),expected[] 和 expectedEmbedding[] 拼接传入；出来后再行分割

            int paramSize = featureIndex.size() + featureIndex.sizeEmbedding();

            double [] alphaCombine = new double[alpha.length+ alphaEmbedding.length];
            double [] expectedCombine = new double[threads.get(0).expected.length + threads.get(0).expectedEmbedding.length];

            int li = 0 ;
            for(; li<alpha.length;li++){
                alphaCombine[li] = alpha[li];
            }
            for(int i = 0; i < alphaEmbedding.length; i++){
                alphaCombine[li+i] = alphaEmbedding[i];
            }


            li = 0 ;
            for(; li<alpha.length;li++){
                expectedCombine[li] = threads.get(0).expected[li];
            }
            for(int i = 0; i < alphaEmbedding.length; i++){
                expectedCombine[li+i] = threads.get(0).expectedEmbedding[i];
            }

            StringBuffer sb = new StringBuffer();
            for(int i =0 ;i<expectedCombine.length;i++){
                sb.append(expectedCombine[i] + ",");
            }
            //System.out.println("expectedCombine:" + expectedCombine.length);
            //System.out.println("expectedCombine:" + sb.toString());

            sb = new StringBuffer();
            for(int i =0 ;i<alphaCombine.length;i++){
                sb.append(alphaCombine[i] + ",");
            }
            //System.out.println("alphaCombine:" + alphaCombine.length);
            //System.out.println("alphaCombine:" + sb.toString());


            int ret = lbfgs.optimize(paramSize,
                    alphaCombine, threads.get(0).obj, expectedCombine, orthant, C);
            //System.out.println("ret:" + ret);

            li = 0;
            for(; li<alpha.length;li++){
                alpha[li] = alphaCombine[li];
            }
            for(int i = 0; i <alphaEmbedding.length; i++){
                alphaEmbedding[i] = alphaCombine[li++];
            }

            li = 0 ;
            for(; li<alpha.length;li++){
                threads.get(0).expected[li] = expectedCombine[li];
            }
            for(int i = 0; i < alphaEmbedding.length; i++){
                threads.get(0).expectedEmbedding[i] = expectedCombine[li++];
            }

            if (ret <= 0) {
                return false;
            }
        }

        System.out.println("run over! ");
        executor.shutdown();
        try {
            executor.awaitTermination(-1, TimeUnit.SECONDS);
        } catch(Exception e) {
            e.printStackTrace();
            System.err.println("fail waiting executor to shutdown");
        }
        return true;
    }

    public boolean runMIRA(List<TaggerImpl> x,
                           EncoderFeatureIndex featureIndex,
                           double[] alpha,
                           int maxItr,
                           double C,
                           double eta,
                           int shrinkingSize,
                           int threadNum) {
        Integer[] shrinkArr = new Integer[x.size()];
        Arrays.fill(shrinkArr, 0);
        List<Integer> shrink = Arrays.asList(shrinkArr);
        Double[] upperArr = new Double[x.size()];
        Arrays.fill(upperArr, 0.0);
        List<Double> upperBound = Arrays.asList(upperArr);
        Double[] expectArr = new Double[featureIndex.size()];
        List<Double> expected = Arrays.asList(expectArr);

        if (threadNum > 1) {
            System.err.println("WARN: MIRA does not support multi-threading");
        }
        int converge = 0;
        int all = 0;
        for (int i = 0; i < x.size(); i++) {
            all += x.get(i).size();
        }

        for (int itr = 0; itr < maxItr; itr++) {
            int zeroone = 0;
            int err = 0;
            int activeSet = 0;
            int upperActiveSet = 0;
            double maxKktViolation = 0.0;

            for (int i = 0; i < x.size(); i++) {
                if (shrink.get(i) >= shrinkingSize) {
                    continue;
                }
                ++activeSet;
                for (int t = 0; t < expected.size(); t++) {
                    expected.set(t, 0.0);
                }
                double costDiff = x.get(i).collins(expected);
                int errorNum = x.get(i).eval();
                err += errorNum;
                if (errorNum != 0) {
                    ++zeroone;
                }
                if (errorNum == 0) {
                    shrink.set(i, shrink.get(i) + 1);
                } else {
                    shrink.set(i, 0);
                    double s = 0.0;
                    for (int k = 0; k < expected.size(); k++) {
                        s += expected.get(k) * expected.get(k);
                    }
                    double mu = Math.max(0.0, (errorNum - costDiff) / s);

                    if (upperBound.get(i) + mu > C) {
                        mu = C - upperBound.get(i);
                        upperActiveSet++;
                    } else {
                        maxKktViolation = Math.max(errorNum - costDiff, maxKktViolation);
                    }

                    if (mu > 1e-10) {
                        upperBound.set(i, upperBound.get(i) + mu);
                        upperBound.set(i, Math.min(C, upperBound.get(i)));
                        for (int k = 0; k < expected.size(); k++) {
                            alpha[k] += mu * expected.get(k);
                        }
                    }
                }
            }
            double obj = 0.0;
            for (int i = 0; i < featureIndex.size(); i++) {
                obj += alpha[i] * alpha[i];
            }

            StringBuilder b = new StringBuilder();
            b.append("iter=").append(itr);
            b.append(" terr=").append(1.0 * err / all);
            b.append(" serr=").append(1.0 * zeroone / x.size());
            b.append(" act=").append(activeSet);
            b.append(" uact=").append(upperActiveSet);
            b.append(" obj=").append(obj);
            b.append(" kkt=").append(maxKktViolation);
            System.out.println(b.toString());

            if (maxKktViolation <= 0.0) {
                for (int i = 0; i < shrink.size(); i++) {
                    shrink.set(i, 0);
                }
                converge++;
            } else {
                converge = 0;
            }
            if (itr > maxItr || converge == 2) {
                break;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("incorrect No. of args");
            return;
        }
        String templFile = args[0];
        String trainFile = args[1];
        String modelFile = args[2];
        String embeddingFile = args[3];
        Encoder enc = new Encoder();
        long time1 = new Date().getTime();
        String embeddingDefModule = "MAX";
        if (!enc.learn(templFile, trainFile, modelFile, embeddingFile,false, 100000, 1, 0.0001, 1.0, 1, 20, Algorithm.CRF_L2,embeddingDefModule)) {
            System.err.println("error training model");
            return;
        }
        System.out.println(new Date().getTime() - time1);
    }
}
