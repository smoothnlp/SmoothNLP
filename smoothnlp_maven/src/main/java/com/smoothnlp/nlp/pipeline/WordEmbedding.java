package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.UtilFns;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

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
        WordEmbedding wordEmbedding = new WordEmbedding("/Users/junyin/polyglot-zh.txt");
        System.out.println(UtilFns.toJson(wordEmbedding.process("的")));
    }

}
