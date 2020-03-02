package com.smoothnlp.nlp.basic;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import com.smoothnlp.nlp.SmoothNLP;

public class MultiDictionary implements IDictionary {

    private IDictionary[] dictionaries;
    public MultiDictionary(IDictionary[] dictionaries){
        this.dictionaries = dictionaries;
    }

    public List<MatchResult> find(String inputText){
        List<MatchResult> resList = new LinkedList<>();
        for (IDictionary dict: this.dictionaries){
            resList.addAll(dict.find(inputText));
        }

        List<MatchResult> deDupledList = new LinkedList<>();
        PriorityQueue<MatchResult> pqMatches = new PriorityQueue<>(Collections.reverseOrder());
        pqMatches.addAll(resList);
        List<int[]> trackedRanges = new LinkedList<>();
        while(!pqMatches.isEmpty()) {
            MatchResult matchResult = pqMatches.poll();
            boolean entityOverlaped = false;
            for (int[] range: trackedRanges){
                if (matchResult.start>=range[0] & matchResult.end<=range[1]){
                    entityOverlaped = true;
                    break;
                }
            }
            if (!entityOverlaped){
                deDupledList.add(matchResult);
//                System.out.println(" -- get added: "+UtilFns.toJson(matchResult));
            }

        }

        return deDupledList;
    }

    public List<MatchResult> find(String inputText, List<String> libraries){
        List<MatchResult> resList = new LinkedList<>();
        for (IDictionary dict: this.dictionaries){
            resList.addAll(dict.find(inputText,libraries));
        }

        List<MatchResult> deDupledList = new LinkedList<>();
        PriorityQueue<MatchResult> pqMatches = new PriorityQueue<>(Collections.reverseOrder());
        pqMatches.addAll(resList);
        List<int[]> trackedRanges = new LinkedList<>();
        while(!pqMatches.isEmpty()) {
            MatchResult en = pqMatches.poll();
            boolean entityOverlaped = false;
            for (int[] range: trackedRanges){
                if (en.start>=range[0] & en.end<=range[1]){
                    entityOverlaped = true;
                    break;
                }
            }
            if (!entityOverlaped){
                deDupledList.add(en);
                int[] newRange ={en.start,en.end};
                trackedRanges.add(newRange);
            }

        }

        return deDupledList;
    }

    public static void main(String[] args){
//        MultiDictionary mner = new MultiDictionary(new IDictionary[]{SmoothNLP.trieDict,SmoothNLP.regexDict});
//        System.out.println(UtilFns.toJson(mner.find("深圳市太阳卡通策划设计有限公司成立于2001年03月07日")));
    }

}
