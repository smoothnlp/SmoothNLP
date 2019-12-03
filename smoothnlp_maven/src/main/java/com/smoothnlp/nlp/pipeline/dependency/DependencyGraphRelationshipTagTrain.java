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

    public DependencyGraphRelationshipTagTrain(){

    }

    public static DMatrix readCoNLL2DMatrix(String CoNLLFile,int negSampleRate) throws IOException {
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
        ArrayList<String[]> labelCollection = new ArrayList<String[]>();



        for (String[] cdoc: conll_docs){
            try{
                CoNLLDependencyGraph conllGraph =CoNLLDependencyGraph.parseLines2Graph(cdoc);
//                conllGraph.setPosNegSampleRate(negSampleRate);
//                conllGraph.selectIndex();

                conllGraph.selectTagIndex();
                labelCollection.add(conllGraph.getAllTagLabel());

                labelCollection.add(conllGraph.getAllTagLabel());
                ftrCollection.add(conllGraph.buildAllFtrs());
            }catch (Exception e){
                System.out.println(Arrays.toString(cdoc));
                System.out.println(e);
                break;
            }
        }

        int record_counter= 0;
        int record_counter2 = 0;
        for (String[] labelc: labelCollection){record_counter+=labelc.length;}
        for (Float[][] ftrc: ftrCollection){record_counter2+=ftrc.length;}
        System.out.println(String.format("~~ Number of records: %d %d ~~",record_counter,record_counter2));

        String[] labels_array = new String[record_counter];
        int counter=0;
        for (String[] labelc: labelCollection){
            for (String f: labelc){
                labels_array[counter] = f;
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
//            dmatrix.setLabel(labels_array);
            return dmatrix;
        }catch(XGBoostError e){
            System.out.println(e);
        }
        return null;

    }

}
