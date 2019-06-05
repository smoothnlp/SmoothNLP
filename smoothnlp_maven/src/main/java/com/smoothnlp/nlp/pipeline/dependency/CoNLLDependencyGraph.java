package com.smoothnlp.nlp.pipeline.dependency;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.CoNLLToken;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.basic.UtilFns;

import java.util.*;

import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;

public class CoNLLDependencyGraph {

    /**
     * construct dependencies as a graph
     */

    public CoNLLToken[] tokens;
    private int nodeSize;
    public float[][] edgeScores;

    public CoNLLDependencyGraph(CoNLLToken[] tokens){
        this.tokens = tokens;
        this.nodeSize = this.tokens.length;
    }

    public CoNLLDependencyGraph(List<SToken> stokens){
        CoNLLToken[] ctokens = new CoNLLToken[stokens.size()+1];
        ctokens[0] = CoNLLToken.ROOT; // Please REMEMBER to add dummy ROOT for constructing CoNLLGraph
        for (int i = 0; i<stokens.size(); i++){
            SToken stoken = stokens.get(i);
            ctokens[i+1] = new CoNLLToken(stoken.getToken(),stoken.getPostag(),i+1);
        }
        this.tokens = ctokens;
        this.nodeSize = this.tokens.length;
    }

    public String toString(){
        return UtilFns.join("\n",this.tokens);
    }

    public void setEdgeScores(float[][] edgeScores){
        this.edgeScores = edgeScores;
    }


    public List<DependencyRelationship> parseDependencyRelationships(){
        /**
         * figure out dependency using "maximal spanning tree" algorithm.
         * Implemented with a PriorityQuee
         */

        PriorityQueue<ScoreEdge> edgePQ = new PriorityQueue<ScoreEdge>();
        for (int i = 0; i<this.nodeSize; i++){
            for (int j = 1; j<this.nodeSize; j++){
                edgePQ.add(new ScoreEdge(i,j,this.edgeScores[i][j]));
            }
        }

        // construct a set to keep track of unreached indexes
        HashSet<Integer> unreachedIndexes = new HashSet<Integer>();
        for (int i = 1; i<this.nodeSize; i++) { unreachedIndexes.add(i);};

        List<DependencyRelationship> relationships = new ArrayList<DependencyRelationship>();

        while (!unreachedIndexes.isEmpty()){
            ScoreEdge selectedEdge = edgePQ.poll();
            if (unreachedIndexes.contains(selectedEdge.target)){
                relationships.add(new DependencyRelationship(selectedEdge.source,selectedEdge.target,this.tokens[selectedEdge.source],this.tokens[selectedEdge.target]));
                unreachedIndexes.remove(selectedEdge.target);
            }
        }

        return relationships;
    }


    public int size(){
        return this.nodeSize;
    }

    public Float[] buildFtrs(int dependentIndex, int targetIndex){
        /**
         * the feature building might be simple in early stages
         * for latter development, please reference: https://github.com/orgs/smoothnlp/teams/let-s-survive/discussions/6
         */
        List<Float> ftrs = new LinkedList<>();
        float dhashcode = this.tokens[dependentIndex].getToken().hashCode();
        float thashcode = this.tokens[targetIndex].getToken().hashCode();
        ftrs.add(dhashcode);
        ftrs.add(thashcode);

        float dpostag_hcode = this.tokens[dependentIndex].getPostag().hashCode();
        float tpostag_hcode = this.tokens[targetIndex].getPostag().hashCode();
        ftrs.add(dpostag_hcode);
        ftrs.add(tpostag_hcode);
        ftrs.add((float) dependentIndex - targetIndex);  // 两者token之间的位置

        // 添加Embedding 特征
        //System.out.println(String.format("dependent token : %s",this.tokens[dependentIndex].getToken()));
        //System.out.println(String.format("target token : %s",this.tokens[targetIndex].getToken()));

        float[] dependent_vec = SmoothNLP.WORDEMBEDDING_PIPELINE.process(this.tokens[dependentIndex].getToken());
        float[] target_vec = SmoothNLP.WORDEMBEDDING_PIPELINE.process(this.tokens[targetIndex].getToken());



        for (float f: dependent_vec) {ftrs.add(f);};
        for (float f: target_vec) {ftrs.add(f);};

        return ftrs.toArray(new Float[ftrs.size()]);
    }

    public Float getLabel(int dependentIndex, int targetIndex){
        float label = this.tokens[targetIndex].getDependentIndex()==dependentIndex ? 1.0f : 0.0f ;
        return label;
    }

    public Float[][] buildAllFtrs(){
        /**
         * build features for all token pair
         */
        int ftr_size = buildFtrs(0,0).length;
        Float[][] all_ftrs = new Float[(this.nodeSize)*(this.nodeSize)][ftr_size];
        for (int i = 0; i< this.nodeSize ;i++){
            for (int j =0; j< this.nodeSize; j++){
                all_ftrs[i*(this.nodeSize)+j] = buildFtrs(i,j);
            }
        }
        return all_ftrs;
    }

    public Float[] getAllLabel(){
        /**
         * return dependency connection labels for all token pairs
         */
        Float[] labels = new Float[(this.nodeSize)*(this.nodeSize)];
        for (int i = 0; i< this.nodeSize;i++){
            for (int j =0; j< this.nodeSize; j++){
                labels[i*(this.nodeSize)+j] = getLabel(i,j);
            }
        }
        return labels;
    }

    public static CoNLLDependencyGraph parseLines2Graph(String[] conllLines){
        /**
         * parse lines in CoNLLX/U format into a Dependency Graph
         */
        CoNLLToken[] tokens = new CoNLLToken[conllLines.length+1];
        tokens[0] = CoNLLToken.ROOT;
        for (int i = 0; i< conllLines.length; i++){
            tokens[i+1] = CoNLLToken.parseCoNLLLine(conllLines[i]);
        }
        return new CoNLLDependencyGraph(tokens);
    }

    public static void main(String[] args){
        String[] doc2 = new String[]{
                "1\t中国\t_\tNR\tNR\t_\t2\tnn\t_\t_\n",
                "2\t经济\t_\tNN\tNN\t_\t3\tnn\t_\t_\n",
                "3\t简讯\t_\tNN\tNN\t_\t0\troot\t_\t_"};

        String[] doc = new String[]{"1\t近\t_\tAD\tAD\t_\t3\tadvmod\t_\t_\n" ,
                "2\t六\t_\tCD\tCD\t_\t3\tnummod\t_\t_\n" ,
                "3\t年\t_\tM\tM\t_\t5\tnsubj\t_\t_\n" ,
                "4\t山西省\t_\tNR\tNR\t_\t5\tnsubj\t_\t_\n" ,
                "5\t利用\t_\tVV\tVV\t_\t0\troot\t_\t_\n" ,
                "6\t外资\t_\tNN\tNN\t_\t5\tdobj\t_\t_\n" ,
                "7\t逾\t_\tAD\tAD\t_\t9\tadvmod\t_\t_\n" ,
                "8\t十亿\t_\tCD\tCD\t_\t9\tnummod\t_\t_\n" ,
                "9\t美元\t_\tM\tM\t_\t5\trange\t_\t_"

        };

        CoNLLDependencyGraph g = parseLines2Graph(doc);

//        System.out.println(g);
        System.out.println(g.buildAllFtrs().length);
        System.out.println(g.buildAllFtrs()[0].length);
        System.out.println(g.getAllLabel().length);

        System.out.println(g.buildFtrs(0,1).length);
        System.out.println(Arrays.toString(g.buildFtrs(0,1)));
        System.out.println(UtilFns.toJson(g.buildAllFtrs()));
        System.out.println(Arrays.toString(g.getAllLabel()));

//        PriorityQueue<ScoreEdge> pqSE = new PriorityQueue<ScoreEdge>();
//        pqSE.add(new ScoreEdge(0,0,1.0f));
//        pqSE.add(new ScoreEdge(0,0,2.0f));
//        System.out.println(pqSE.poll().score);




    }

}
