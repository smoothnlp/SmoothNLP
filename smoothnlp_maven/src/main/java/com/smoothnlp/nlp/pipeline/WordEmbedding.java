package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.CoNLLToken;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.basic.UtilFns;
import scala.Array;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class WordEmbedding implements IEmbedding{
    public static Map<String, float[]> wordEmbedding ;
    public int embedding_size;
    public static String sep = " ";

    // 加载embedding dictionary
    public WordEmbedding(){
        this(SmoothNLP.WordEmbedding_MODEL);
    }

    public WordEmbedding(String wordEmbeddingPath){
        this.wordEmbedding = new HashMap<>();
        load(wordEmbeddingPath);
    }


    private void load(String path){

        try{
            InputStream ioInput = SmoothNLP.IOAdaptor.open(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(ioInput));
            int index=0 , dimen = 0;
            long start = System.currentTimeMillis();
            while ((reader.ready())) {
                String line = reader.readLine();
                line = line.trim();
                //line = line.replace("  "," ");
                String[] splits = line.split(sep);
                if(splits.length == 2 ){
                    continue;
                }
                index ++;
                if(index == 1){
                    dimen = splits.length-1;
                }
                String word = splits[0];
                float[] embedding = new float[dimen];
                for(int i = 1; i<splits.length; i++){
                    embedding[i-1] = Float.parseFloat(splits[i]);
                }
                wordEmbedding.put(word, embedding);

            }
            System.out.println(dimen+" dimension size");
            this.embedding_size = dimen;
            System.out.println("Embedding 读入条数" + index + ", 耗时" + (System.currentTimeMillis()-start) + "ms");
        }catch (IOException e){
            SmoothNLP.LOGGER.severe(e.getMessage());
        }
    }

    /**
     * 获取word embedding结果
     * @param input
     * @return
     */
    public float[] processToken(String input){
        if(wordEmbedding.containsKey(input)) {
            return wordEmbedding.get(input);
        }else{
            LinkedList<float[]> vectors = new LinkedList<>();
            char[] charArray = input.toCharArray();
            for (char c : charArray){
                if (wordEmbedding.containsKey(Character.toString(c))) {
                    vectors.add(wordEmbedding.get(Character.toString(c)));
                }
            }
            if (vectors.size()==0){
                float[] dummy_vec = new float[this.embedding_size];
                Arrays.fill(dummy_vec,0f);
                return dummy_vec;
            }else{
                float[] agg_vector = new float[this.embedding_size];
                Arrays.fill(agg_vector,0f);
                for (int i=0;i<this.embedding_size;i++){
                    for (float[] vec: vectors){
                        agg_vector[i]+=vec[i];
                    }
                    agg_vector[i] = agg_vector[i]/vectors.size();
                }
                return agg_vector;
            }

        }
    }

    public float[] processToken(CoNLLToken token){
        return processToken(token.token);
    }

    public float[] processToken(SToken token){
        return processToken(token.token);
    }

    public float[] processTokens(CoNLLToken[] tokens){
        float[] vector = new float[this.embedding_size];
        Arrays.fill(vector,-99f);
        for (CoNLLToken token : tokens){
            float[] tokenVector = this.processToken(token);
            for (int i=0;i<this.embedding_size;i++){
                vector[i]  = Math.max(vector[i],tokenVector[i]);
            }
        }
        return vector;
    }

    public float[] processTokens(List<SToken> tokens){
        float[] vector = new float[this.embedding_size];
        Arrays.fill(vector,-99f);
        for (SToken token : tokens){
            float[] tokenVector = this.processToken(token);
            for (int i=0;i<this.embedding_size;i++){
                vector[i]  = Math.max(vector[i],tokenVector[i]);
            }
        }
        return vector;
    }

    public float[] processSentence(String sentence){
        List<SToken> stokens = SmoothNLP.SEGMENT_PIPELINE.process(sentence);
        return processTokens(stokens);
    }

    public float[] process(String input){
        if(wordEmbedding.containsKey(input)) {
            return wordEmbedding.get(input);
        }else{
            float[] dummy_vec = new float[this.embedding_size];
            Arrays.fill(dummy_vec,0f);
            return dummy_vec;
        }
    }


    /**
     * test path
     */

    public static void main(String[] args){
        WordEmbedding wordEmbedding = new WordEmbedding(SmoothNLP.WordEmbedding_MODEL);
        System.out.println(UtilFns.toJson(wordEmbedding.processSentence("嗅问")));
        System.out.println(UtilFns.toJson(wordEmbedding.processSentence("嗅问将会是一款优秀的产品")));
    }

}
