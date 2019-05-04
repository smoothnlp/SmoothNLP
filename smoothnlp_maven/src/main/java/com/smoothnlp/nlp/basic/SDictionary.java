package com.smoothnlp.nlp.basic;

import com.smoothnlp.nlp.SmoothNLP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SDictionary{
    private List<String> wordList;
    private Pattern patterns;

    public SDictionary(String[] args){
        wordList = new ArrayList<>();
        for (int i = 0; i<args.length;i=i+1){
            String fileName = args[i];
            try {
                InputStream is = SmoothNLP.IOAdaptor.open(fileName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                while(reader.ready()) {
                    String word = reader.readLine();
                    wordList.add(word);
                }
            }catch(IOException e){
                SmoothNLP.LOGGER.severe(e.getMessage());
            }
        }
        patterns = Pattern.compile(UtilFns.join("|",wordList));
    }

    public List<int[]> indicate(String inputText){
        List<int[]> resList = new ArrayList<>();
        Matcher matcher = patterns.matcher(inputText);
        while (matcher.find()){
            resList.add(new int[]{matcher.start(),matcher.end()});
        }
        return resList;
    }

}
