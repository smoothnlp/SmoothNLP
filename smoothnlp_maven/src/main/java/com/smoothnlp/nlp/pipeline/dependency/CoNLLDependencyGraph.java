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
    private int posNegSampleRate = 2;
    private LinkedList<int[]> selectedIndexes = null;

    public CoNLLDependencyGraph(CoNLLToken[] tokens){
        this.tokens = tokens;
        this.nodeSize = this.tokens.length;
    }

    public void setPosNegSampleRate(int rate){
        this.posNegSampleRate = rate;
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
//        ftrs.addAll(getTopN(dependent_vec,5));
//        ftrs.addAll(getTopN(target_vec,5));

        for (float f: dependent_vec) {ftrs.add(f);};
        for (float f: target_vec) {ftrs.add(f);};
        return ftrs.toArray(new Float[ftrs.size()]);
    }

//    public List<Float> getTopN(float[] a, int topn){
//        float[] tempa = a.clone();
//        Arrays.sort(tempa);
//        LinkedList<Float> resultIndexes = new LinkedList<>();
//        for (int i=0;i<topn;i++){
//            float target_value = tempa[i];
//            for (int j = 0; j<a.length;j++){
//                if (target_value == a[j]){
//                    resultIndexes.add((float) j);
//                    break;
//                }
//            }
//        }
//        return resultIndexes;
//    }

    public Float getLabel(int dependentIndex, int targetIndex){
        float label = this.tokens[targetIndex].getDependentIndex()==dependentIndex ? 1.0f : 0.0f ;
        return label;
    }

    public Float[][] buildAllFtrs(){
        /**
         * build features for all token pair
         */
        int ftr_size = buildFtrs(0,0).length;


        if (selectedIndexes==null){
            Float[][] all_ftrs = new Float[(this.nodeSize)*(this.nodeSize)][ftr_size];
            for (int i = 0; i< this.nodeSize ;i++){
                for (int j =0; j< this.nodeSize; j++){
                    all_ftrs[i*(this.nodeSize)+j] = buildFtrs(i,j);
                }
            }
            return all_ftrs;
        }else{
            Float[][] all_ftrs = new Float[selectedIndexes.size()][ftr_size];
            int count = 0;
            for (int[] index : selectedIndexes){
                int i = index[0];
                int j = index[1];
                all_ftrs[count] = buildFtrs(i,j);
                count+=1;
            }
            return all_ftrs;
        }


    }

    /**
     * 由于两两token组合中, label=0 占比高很多, 所以对样本准备做适当undersample
     * @return
     */
    public void selectIndex(){
        this.selectedIndexes = new LinkedList<>();
        LinkedList<int[]> negativePairs = new LinkedList<>();
        for (int i = 0; i< this.nodeSize;i++){
            for (int j =0; j< this.nodeSize; j++){
                if (getLabel(i,j)==1.0f) {
                    this.selectedIndexes.add(new int[]{i, j});
                }else{
                    negativePairs.add(new int[]{i,j});
                }
            }
        }
        Collections.shuffle(negativePairs);
        if (this.posNegSampleRate*this.selectedIndexes.size()<negativePairs.size()){
            this.selectedIndexes.addAll(negativePairs.subList(0,this.posNegSampleRate*selectedIndexes.size()));
        }else{
            selectedIndexes.addAll(negativePairs);
        }
    }

    public Float[] getAllLabel(){
        /**
         * return dependency connection labels for all token pairs
         */


        if (selectedIndexes==null){
            Float[] labels = new Float[(this.nodeSize)*(this.nodeSize)];
            for (int i = 0; i< this.nodeSize;i++){
                for (int j =0; j< this.nodeSize; j++){
                    labels[i*(this.nodeSize)+j] = getLabel(i,j);
                }
            }
            return labels;
        }else{
            Float[] labels = new Float[this.selectedIndexes.size()];
            int count = 0;
            for (int[] index : selectedIndexes){
                int i = index[0];
                int j = index[1];
                labels[count] = getLabel(i,j);
                count+=1;
            }
            return labels;
        }


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

        g.selectIndex();
        System.out.println(UtilFns.toJson(g.selectedIndexes));

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
