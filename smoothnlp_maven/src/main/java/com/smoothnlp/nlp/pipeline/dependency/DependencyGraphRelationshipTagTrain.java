package com.smoothnlp.nlp.pipeline.dependency;

import com.smoothnlp.nlp.SmoothNLP;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.*;

import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;


public class DependencyGraphRelationshipTagTrain {

    public static Map<String, Float> tag2float;
    static {
        tag2float = new HashMap<>();
        tag2float.put("UNKNOWN", 0.0f);
        tag2float.put("dep", 1.0f);
        tag2float.put("punct", 2.0f);
        tag2float.put("nsubj", 3.0f);
        tag2float.put("advmod", 4.0f);
        tag2float.put("root", 5.0f);
        tag2float.put("det", 6.0f);
        tag2float.put("clf", 7.0f);
        tag2float.put("prep", 8.0f);
        tag2float.put("pobj", 9.0f);
        tag2float.put("nn", 10.0f);
        tag2float.put("lobj", 11.0f);
        tag2float.put("dobj", 12.0f);
        tag2float.put("nummod", 13.0f);
        tag2float.put("range", 14.0f);
        tag2float.put("conj", 15.0f);
        tag2float.put("rcmod", 16.0f);
        tag2float.put("assmod", 17.0f);
        tag2float.put("assm", 18.0f);
        tag2float.put("asp", 19.0f);
        tag2float.put("cc", 20.0f);
        tag2float.put("cpm", 21.0f);
        tag2float.put("tmod", 22.0f);
        tag2float.put("etc", 23.0f);
        tag2float.put("prtmod", 24.0f);
        tag2float.put("amod", 25.0f);
        tag2float.put("attr", 26.0f);
        tag2float.put("ordmod", 27.0f);
        tag2float.put("top", 28.0f);
        tag2float.put("ccomp", 29.0f);
        tag2float.put("prnmod", 30.0f);
        tag2float.put("loc", 31.0f);
        tag2float.put("vmod", 32.0f);
        tag2float.put("rcomp", 33.0f);
        tag2float.put("pccomp", 34.0f);
        tag2float.put("lccomp", 35.0f);
        tag2float.put("nsubjpass", 36.0f);
        tag2float.put("pass", 37.0f);
        tag2float.put("xsubj", 38.0f);
        tag2float.put("mmod", 39.0f);
        tag2float.put("dvpmod", 40.0f);
        tag2float.put("dvpm", 41.0f);
        tag2float.put("ba", 42.0f);
        tag2float.put("comod", 43.0f);
        tag2float.put("neg", 44.0f);
        tag2float.put("cop", 45.0f);
        tag2float.put("plmod", 46.0f);
    }


    public DependencyGraphRelationshipTagTrain(){

    }

    public static List<String[]> file2lines(String CoNLLFile) throws IOException{
        InputStream in = SmoothNLP.IOAdaptor.open(CoNLLFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = null;
        List<String[]> conll_docs = new ArrayList<String[]>();
        ArrayList<String> conll_document = new ArrayList<String>();
        while ((line = reader.readLine()) != null){
            if (line.equals("")){
                conll_docs.add(conll_document.toArray(new String[conll_document.size()]));
                conll_document = new ArrayList<String>();
            }else{
                conll_document.add(line);
            }
        }
        conll_docs.add(conll_document.toArray(new String[conll_document.size()]));

        return conll_docs;
    }

    public static DMatrix readCoNLL2DMatrix(String CoNLLFile) throws IOException {

        List<String[]> conll_docs = file2lines(CoNLLFile);

        System.out.println(String.format("~~~ Done with reading all CoNLL documents %d ~~~", conll_docs.size()));

        ArrayList<Float[][]> ftrCollection = new ArrayList<Float[][]>();
        ArrayList<String[]> labelCollection = new ArrayList<String[]>();

        int counter_line = 0;

        for (String[] cdoc: conll_docs){

            // 建立dependency Graph 并且 生成对应的特征与label
            CoNLLDependencyGraph conllGraph =CoNLLDependencyGraph.parseLines2Graph(cdoc);
            labelCollection.add(conllGraph.getAllTagLabel());
            ftrCollection.add(conllGraph.buildAllTagFtrs());

            counter_line+=1;
            if (counter_line % 100==0){
                System.out.print(counter_line+"...");
            }

            // 4 debug only, 检查feature 和 label添加是否正确
//            System.out.print("graph size: ");
//            System.out.println(conllGraph.size());
//            System.out.print("label size: ");
//            System.out.println(conllGraph.getAllTagLabel().length);
//            System.out.print("feature size: ");
//            System.out.println(conllGraph.buildAllTagFtrs().length);

        }
        System.out.println();

        int record_counter= 0;
        int record_counter2 = 0;
        for (String[] labelc: labelCollection){record_counter+=labelc.length;}
        for (Float[][] ftrc: ftrCollection){record_counter2+=ftrc.length;}
        System.out.println(String.format("~~ Number of records: %d %d ~~",record_counter,record_counter2));

        String[] labels_array = new String[record_counter];
        int counter=0;
        for (String[] labelc: labelCollection){
            for (String f: labelc){
                if (f== null){
                    System.out.println("label is null!");
                }
                labels_array[counter] = f;
                counter+=1;
            }
        }

//        String[] unique = Arrays.stream(labels_array).distinct().toArray(String[]::new);
//        System.out.println(unique);
//        for (String s : unique){
//            System.out.println(s);
//        }

        float[] labels_float= new float[labels_array.length];
        for (int i = 0;i < labels_array.length; i++){

            if (tag2float.containsKey(labels_array[i])){
                labels_float[i] = tag2float.get(labels_array[i]);
            }else{
                System.out.print("unknown label ");
                System.out.println(labels_array[i]);
                labels_float[i] = 0.0f;
            }
            // 4 debug only; 检查 标签tag -> 对应label(float)对应情况
//            System.out.print("label value: ");
//            System.out.print(labels_array[i]);
//            System.out.println(labels_float[i]);
        }

        System.out.println(String.format("~~ Flattened labels: %d ~~",counter));

        int ftr_size = ftrCollection.get(0)[0].length;
        float[] ftrs_array = new float[record_counter*ftr_size];
        counter = 0;

        for (Float[][] doc_ftr:ftrCollection){
            for (Float[] pair_ftr:doc_ftr){
                for (Float f: pair_ftr){
                    ftrs_array[counter]=(f != null ? f : Float.NaN);
                    counter+=1;
                }
            }
        }

        System.out.println(String.format("~~ Flattened ftrs: %d ~~",counter));

        System.out.println(String.format("~~ Ftr and Label preparation ready: %d %d", labels_array.length, ftrs_array.length));
        System.out.println(String.format("~~ Ftr Size: %d ~~",ftr_size));
        try{
            final DMatrix dmatrix = new DMatrix(ftrs_array,record_counter,ftr_size,Float.NaN);
            dmatrix.setLabel(labels_float);
            return dmatrix;
        }catch(XGBoostError e){
            System.out.println(e);
        }
        return null;
    }

    public static void trainXgbModel(String trainFile, String devFile, String modelAddr, int nround, int earlyStop,int nthreads ) throws IOException{
        final DMatrix trainMatrix = readCoNLL2DMatrix(trainFile);
        final DMatrix devMatrix = readCoNLL2DMatrix(devFile);
        try{
            Map<String, Object> params = new HashMap<String, Object>() {
                {
                    put("nthread", nthreads);
                    put("max_depth", 6);
                    put("silent", 0);
                    put("objective", "multi:softprob");
                    put("colsample_bytree",0.95);
                    put("colsample_bylevel",0.95);
                    put("eta",0.2);
                    put("subsample",0.95);
                    put("lambda",1.0);

                    // tree methods for regulation
                    put("min_child_weight",5);
                    put("max_leaves",128);

                    // other parameters
                    // "objective" -> "multi:softmax", "num_class" -> "6"

                    put("eval_metric", "merror");
                    put("tree_method","approx");
                    put("num_class",tag2float.size());

                    put("min_child_weight",5);
                }
            };
            Map<String, DMatrix> watches = new HashMap<String, DMatrix>() {
                {
                    put("train", trainMatrix);
                    put("dev",devMatrix);
                }
            };
            Booster booster = XGBoost.train(trainMatrix, params, nround, watches, null, null,null,earlyStop);
            OutputStream outstream = SmoothNLP.IOAdaptor.create(modelAddr);
            booster.saveModel(outstream);



        }catch(XGBoostError e){
            System.out.println(e);
        }
    }



    public static void main (String[] args) throws IOException {
//        readCoNLL2DMatrix("dev_sample.conllx");

        if (args.length==3){
            trainXgbModel(args[0],args[1],args[2],20,10,8);
        }else{
            trainXgbModel(args[0],args[1],args[2], Integer.parseInt(args[3]),Integer.parseInt(args[4]),Integer.parseInt(args[5]));
        }

//        trainXgbModel("dev.conllx","dev.conllx","dp_tagmodel.bin",10,10,2);

        // put in train, valid, model destination
//        trainXgbModel("dev.conllx","test.conllx","dpmodel_tem.bin",1);
//        if (args.length==3){
//            trainXgbModel(args[0],args[1],args[2],20,1,10);
//        }else{
//            trainXgbModel(args[0],args[1],args[2], Integer.parseInt(args[3]),Integer.parseInt(args[4]),Integer.parseInt(args[5]));
//        }
    }



}
