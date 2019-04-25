package com.smoothnlp.nlp.pipeline.dependency;

import com.smoothnlp.nlp.basic.CoNLLToken;
import com.smoothnlp.nlp.basic.UtilFns;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class CoNLLDependencyGraph {

    public CoNLLToken[] tokens;
    private int nodeSize;

    public CoNLLDependencyGraph(CoNLLToken[] tokens){
        this.tokens = tokens;
        this.nodeSize = this.tokens.length;
    }

    public String toString(){
        return UtilFns.join("\n",this.tokens);
    }

    public List<Float> buildFtrs(int dependentIndex, int targetIndex){
        /**
         * the feature building might be simple in early stages
         * for latter development, please reference: https://github.com/orgs/smoothnlp/teams/let-s-survive/discussions/6
         */
        List<Float> ftrs = new ArrayList<Float>();
        float dhashcode = this.tokens[dependentIndex].getToken().hashCode();
        float thashcode = this.tokens[targetIndex].getToken().hashCode();
        ftrs.add(dhashcode);
        ftrs.add(thashcode);

        float dpostag_hcode = this.tokens[dependentIndex].getPostag().hashCode();
        float tpostag_hcode = this.tokens[targetIndex].getPostag().hashCode();
        ftrs.add(dpostag_hcode);
        ftrs.add(tpostag_hcode);
        ftrs.add((float) dependentIndex - targetIndex);
        return ftrs;
    }

    public boolean getLabel(int dependentIndex, int targetIndex){
        return this.tokens[targetIndex].getDependentIndex()==dependentIndex;
    }

    public static CoNLLDependencyGraph parseLines2Graph(String[] conllLines){
        CoNLLToken[] tokens = new CoNLLToken[conllLines.length+1];
        tokens[0] = CoNLLToken.ROOT;
        for (int i = 0; i< conllLines.length; i++){
            tokens[i+1] = CoNLLToken.parseCoNLLLine(conllLines[i]);
        }
        return new CoNLLDependencyGraph(tokens);
    }

    public static void main(String[] args){
        String[] doc = new String[]{"1\t中国\t_\tNR\tNR\t_\t2\tnn\t_\t_\n",
                "2\t经济\t_\tNN\tNN\t_\t3\tnn\t_\t_\n",
                "3\t简讯\t_\tNN\tNN\t_\t0\troot\t_\t_"};

        CoNLLDependencyGraph g = parseLines2Graph(doc);
        System.out.println(g);

        System.out.println(g.buildFtrs(0,2));

    }

}
