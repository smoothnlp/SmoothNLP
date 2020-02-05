package com.smoothnlp.nlp.model.crfagu;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

public class EmbeddingImpl {

    private HashMap<String,float[]> embeddingVector;  // string , float[]
    private String splitRegex = "[\t| ]";
    private int vsize;  // embedding size
    private static String DEFAULT_VALUE_MODE = "MAX"; //support MAX , AVG , ZERO, SPLIT
    private float[] defaultEmbeddingVector;

    public EmbeddingImpl(){
        embeddingVector = new HashMap<String, float[]>();
        calculateDefaultEmbeddingVector();
    }
    public EmbeddingImpl(String inputFile,String embeddingDefMode){
        this();
        loadEmbedding(inputFile);
        DEFAULT_VALUE_MODE = embeddingDefMode.toUpperCase();
        calculateDefaultEmbeddingVector();
    }
    public EmbeddingImpl(String inputFile,String embeddingDefMode, String splitRegex){
        this();
        setSplitRegex(splitRegex);
        loadEmbedding(inputFile);
        DEFAULT_VALUE_MODE = embeddingDefMode;
        calculateDefaultEmbeddingVector();
    }

    public void setSplitRegex(String splitRegex){
        this.splitRegex = splitRegex;
    }
    public int getVsize(){
        return vsize;
    }

    public void loadEmbedding(String inputFile){
        try{
            InputStreamReader ifs = new InputStreamReader(new FileInputStream(inputFile));
            BufferedReader br = new BufferedReader(ifs);
            loadEmbedding(br);
            br.close();
        }catch (Exception e ){
            e.printStackTrace();
            System.err.println("br error");
        }
    }

    public void loadEmbedding(BufferedReader br){
        try{
            String line;
            line = br.readLine();
            String [] cols = line.split(splitRegex);
            int xsize = Integer.parseInt(cols[cols.length -1]);
            vsize = xsize;
            while(true){
                line = br.readLine();
                if(line!=null){
                    add(line);
                }else{
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.err.println("Error reading stream");
        }
    }

    public boolean add(String line){
        String [] cols = line.split(splitRegex);
        if (vsize != cols.length -1){
            System.err.println("# x size=" + cols.length+" and xsize=" + vsize + ",The line is " + line);
            return false;
        }
        String key = cols[0];
        if(embeddingVector.containsKey(key)){
            System.err.println(key + " is repeat");
            return false;
        }
        float [] embedding = new float[vsize];
        for (int i =0; i <vsize; i++){
            embedding[i] = Float.parseFloat(cols[i+1]);
        }
        embeddingVector.put(key,embedding);
        return true;
    }

    /**
     * 重要，根据输入的str, 返回对应embedding vector; 如果无此str,则返回一个固定size(dim size) 的vector;
     * @param key
     * @return
     */
    public float[] getStrEmbedding(String key){
        if(embeddingVector.containsKey(key)){
            return embeddingVector.get(key);
        }else if(DEFAULT_VALUE_MODE == "SPLIT"){
            return getWordSplitEmbeddingVector(key);
        }else{
            return getDefaultEmbeddingVector();
        }
    }

    public float[] getArrayStrEmbedding(List<String> Strs){
        int size = Strs.size() * getVsize();
        float[] vector = new float[size];
        int desPos = 0;
        for(String Str: Strs){
            float[] vs = getStrEmbedding(Str);
            System.arraycopy(vs, 0 , vector, desPos, getVsize());
            desPos += getVsize();
        }
        return vector;
    }

    public float[] getDefaultEmbeddingVector(){
        return this.defaultEmbeddingVector;
    }

    public void calculateDefaultEmbeddingVector(){
        float [] maxVector = new float[vsize];
        float [] avgVector = new float[vsize];
        float [] defVector = new float[vsize];

        for(int i=0; i<vsize; i++){
            defVector[i] = 0;
            maxVector[i] = Float.NEGATIVE_INFINITY ;
            avgVector[i] = 0 ;
        }
        for(String key:embeddingVector.keySet()){
            float[] vector = embeddingVector.get(key);
            for(int i = 0 ; i<vsize;i++){
                if(vector[i]> maxVector[i]){
                    maxVector[i] = vector[i];
                }
                avgVector[i] += vector[i];
            }
        }
        if (DEFAULT_VALUE_MODE.equals("MAX")){
            this.defaultEmbeddingVector = maxVector;
        }else if(DEFAULT_VALUE_MODE.equals("AVG")){
            for(int i = 0 ;i<vsize; i++){
                avgVector[i] = avgVector[i]/embeddingVector.size();
            }
            this.defaultEmbeddingVector = avgVector;
        }else{
            this.defaultEmbeddingVector = defVector;
        }
    }

    public float[] getWordSplitEmbeddingVector(String str){
        float [] splitVector = new float[vsize];
        Arrays.fill(splitVector,0);
        int count = 0 ;
        for(int i = 0 ;i<str.length();i++){
            String substr = str.substring(i,i+1);
            if(embeddingVector.containsKey(substr)){
                float[] sVector = embeddingVector.get(substr);
                for (int j = 0; j < vsize; j++) {
                    splitVector[j] +=sVector[j];
                }
                count += 1;
            }
        }
        if(count>=1){
            for (int j = 0;j<vsize; j++){
                splitVector[j] = splitVector[j]/count;
            }
        }
        return splitVector;

    }

    public Set<String> getEmbeddingKeySet(){
        return embeddingVector.keySet();
    }

    public HashMap<String, float[]> getEmbeddingVector(){
        return embeddingVector;
    }

    public void setEmbeddingVector(HashMap<String, float[]> embeddingVector){
        this.embeddingVector = embeddingVector;
    }
    public void setVsize(int vsize){
        this.vsize = vsize;
    }
    public void setDefaultValueMode(String defaultValue){
        DEFAULT_VALUE_MODE = defaultValue;
    }
    public String getDefaultValueMode(){
        return DEFAULT_VALUE_MODE;
    }

    public static void main(String[]args){
        String file = "embedding.txt";
        EmbeddingImpl embeddingImpl = new EmbeddingImpl(file,"MAX");
        System.out.println(embeddingImpl.getVsize());

        for(String key:embeddingImpl.embeddingVector.keySet()){
            System.out.println(key);
            float[] value = embeddingImpl.getStrEmbedding(key);
            float sum = 0;
            for(int i=0;i<value.length;i++)
            {
                System.out.print(value[i]+" ");
                sum+=value[i];
            }
            System.out.println("");
            System.out.println("-----");
            System.out.println(sum);
        }

        System.out.println("----------------------");

        for(int i =0;i< embeddingImpl.getVsize();i++){
            System.out.println(embeddingImpl.getDefaultEmbeddingVector()[i]);
        }

        for(int i =0;i< embeddingImpl.getVsize();i++){
            System.out.println(embeddingImpl.getStrEmbedding("一美")[i]);
        }

        ArrayList<String> list = new ArrayList<>();
        list.add("眼镜");
        list.add("si");

        System.out.println(Arrays.toString(embeddingImpl.getArrayStrEmbedding(list)));

        System.out.println("------");
        System.out.println(Arrays.toString(embeddingImpl.getWordSplitEmbeddingVector("si")));
        System.out.println(Arrays.toString(embeddingImpl.getStrEmbedding("si")));
        String si = "si";

        for (int i = 0; i <si.length() ; i++) {
            System.out.println(si.substring(i,i+1));
        }

    }

}
