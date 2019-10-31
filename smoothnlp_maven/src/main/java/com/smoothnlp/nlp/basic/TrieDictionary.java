package com.smoothnlp.nlp.basic;

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie;
import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie.Hit;
import com.smoothnlp.nlp.SmoothNLP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class TrieDictionary implements IDictionary{
    /**
     * Dictionary implemented using "AhoCorasickDoubleArrayTrie"
     */

    private AhoCorasickDoubleArrayTrie<String> acdatTrie;

    public TrieDictionary(Map<String,String> libraryMaps){

        this.acdatTrie = new AhoCorasickDoubleArrayTrie<String>();

        SmoothNLP.LOGGER.info("~~~~ Start Building Trie ~~~~~~~");

        long start,end;
        start = System.currentTimeMillis();

        TreeMap<String, String> map = new TreeMap<String, String>();
        for (String label: libraryMaps.keySet()){
            String fileName = libraryMaps.get(label);
            System.out.println("processing file name "+fileName);
            try {
                InputStream is = SmoothNLP.IOAdaptor.open(fileName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                while(reader.ready()) {
                    String word = reader.readLine();
                    map.put(word,label);
                }

            }catch(IOException e){
                SmoothNLP.LOGGER.severe(e.getMessage());
            }
        }

        end = System.currentTimeMillis();
        SmoothNLP.LOGGER.info(String.format("~~~ Trie Building Done in: %d seconds", end-start/100));

        this.acdatTrie.build(map);

    }

    public List<MatchResult> find(String text){
        List<MatchResult> resList = new LinkedList<>();
        for (Hit hit: this.acdatTrie.parseText(text)){
            resList.add(new MatchResult(hit.begin,hit.end,hit.value.toString()));
        }
        return resList;
    }

    public List<MatchResult> find(String text, List<String> validLabels){

        if (validLabels==null){
            return find(text);
        }

        List<MatchResult> resList = new LinkedList<>();
        for (Hit hit: this.acdatTrie.parseText(text)){
            if (validLabels.contains(hit.value.toString())) {
                resList.add(new MatchResult(hit.begin, hit.end, hit.value.toString()));
            }
        }
        return resList;
    }



    public static void main(String[] args){
        TrieDictionary td = new TrieDictionary(new HashMap<String, String>() {
            {
                put("COMPANY_NAME", "financial_agencies.txt");
                put("FINANCE_METRIX", "financial_metrics.txt");
                put("COMMON","common.txt");
            }
        });

        System.out.println(UtilFns.toJson(td.find("国泰君安与阿里巴巴过去三年的营收")));
        System.out.println(UtilFns.toJson(td.find("大屏手机")));

    }

}
