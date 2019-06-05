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

public class DependencyGraghEdgeCostTrain {

    public DependencyGraghEdgeCostTrain(){

    }

    public static DMatrix readCoNLL2DMatrix(String CoNLLFile) throws IOException {
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

        System.out.println(String.format("~~~ Done with reading all CoNLL documents %d ~~~", conll_docs.size()));

        ArrayList<Float[][]> ftrCollection = new ArrayList<Float[][]>();
        ArrayList<Float[]> labelCollection = new ArrayList<Float[]>();
        for (String[] cdoc: conll_docs){
            try{
                CoNLLDependencyGraph conllGraph =CoNLLDependencyGraph.parseLines2Graph(cdoc);
                labelCollection.add(conllGraph.getAllLabel());
                ftrCollection.add(conllGraph.buildAllFtrs());
            }catch (Exception e){
                System.out.println(Arrays.toString(cdoc));
                System.out.println(e);
                break;
            }
        }

        int record_counter= 0;
        int record_counter2 = 0;
        for (Float[] labelc: labelCollection){record_counter+=labelc.length;}
        for (Float[][] ftrc: ftrCollection){record_counter2+=ftrc.length;}
        System.out.println(String.format("~~ Number of records: %d %d ~~",record_counter,record_counter2));

        float[] labels_array = new float[record_counter];
        int counter=0;
        for (Float[] labelc: labelCollection){
            for (Float f: labelc){
                labels_array[counter] = (f != null ? f : Float.NaN);
                counter+=1;
            }
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
            dmatrix.setLabel(labels_array);
            return dmatrix;
        }catch(XGBoostError e){
            System.out.println(e);
        }
        return null;

    }

    public static void trainXgbModel(String trainFile, String devFile, String modelAddr, int nround) throws IOException{
        final DMatrix trainMatrix = readCoNLL2DMatrix(trainFile);
        final DMatrix devMatrix = readCoNLL2DMatrix(devFile);
        try{
            Map<String, Object> params = new HashMap<String, Object>() {
                {
                    put("eta", 1.0);
                    put("max_depth", 3);
                    put("silent", 0);
                    put("objective", "binary:logistic");
                    put("eval_metric", "logloss");
                    put("tree_method","approx");
                }
            };
            Map<String, DMatrix> watches = new HashMap<String, DMatrix>() {
                {
                    put("train", trainMatrix);
                    put("dev",devMatrix);
                }
            };
            Booster booster = XGBoost.train(trainMatrix, params, nround, watches, null, null);
            OutputStream outstream = SmoothNLP.IOAdaptor.create(modelAddr);
            booster.saveModel(outstream);
        }catch(XGBoostError e){
            System.out.println(e);
        }
    }

    public static void main (String[] args) throws IOException {
//        readCoNLL2DMatrix("dev.conllx");
        // put in train, valid, model destinationß
//        trainXgbModel("dev.conllx","test.conllx","dpmodel_tem.bin",1);
        if (args.length==3){
            trainXgbModel(args[0],args[1],args[2],20);
        }else{
            trainXgbModel(args[0],args[1],args[2], Integer.parseInt(args[3]));
        }

    }

}
