package com.smoothnlp.nlp.pipeline.dependency;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.CoNLLToken;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.basic.UtilFns;

import java.awt.datatransfer.SystemFlavorMap;
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

    public static Map<Float,String> float2tag;
    static {
        float2tag = new HashMap<>();
        float2tag.put(0.0f, "UNKOWN");
        float2tag.put(1.0f, "dep");
        float2tag.put(2.0f, "punct");
        float2tag.put(3.0f, "nsubj");
        float2tag.put(4.0f, "advmod");
        float2tag.put(5.0f, "root");
        float2tag.put(6.0f, "det");
        float2tag.put(7.0f, "clf");
        float2tag.put(8.0f, "prep");
        float2tag.put(9.0f, "pobj");
        float2tag.put(10.0f,"nn");
        float2tag.put(11.0f, "lobj");
        float2tag.put(12.0f, "dobj");
        float2tag.put(13.0f, "nummod");
        float2tag.put(14.0f, "range");
        float2tag.put(15.0f, "conj");
        float2tag.put(16.0f, "rcmod");
        float2tag.put(17.0f, "assmod");
        float2tag.put(18.0f, "assm");
        float2tag.put(19.0f, "asp");
        float2tag.put(20.0f, "cc");
        float2tag.put(21.0f, "cpm");
        float2tag.put(22.0f, "tmod");
        float2tag.put(23.0f, "etc");
        float2tag.put(24.0f, "prtmod");
        float2tag.put(25.0f, "amod");
        float2tag.put(26.0f, "attr");
        float2tag.put(27.0f, "ordmod");
        float2tag.put(28.0f, "top");
        float2tag.put(29.0f, "ccomp");
        float2tag.put(30.0f, "prnmod");
        float2tag.put(31.0f, "loc");
        float2tag.put(32.0f, "vmod");
        float2tag.put(33.0f, "rcomp");
        float2tag.put(34.0f, "pccomp");
        float2tag.put(35.0f, "lccomp");
        float2tag.put(36.0f, "nsubjpass");
        float2tag.put(37.0f, "pass");
        float2tag.put(38.0f, "xsubj");
        float2tag.put(39.0f, "mmod");
        float2tag.put(40.0f, "dvpmod");
        float2tag.put(41.0f, "dvpm");
        float2tag.put(42.0f, "ba");
        float2tag.put(43.0f, "comod");
        float2tag.put(44.0f, "neg");
        float2tag.put(45.0f, "cop");
    }

    public CoNLLDependencyGraph(CoNLLToken[] tokens){
        this.tokens = tokens;
        this.nodeSize = this.tokens.length;
    }

    public void setPosNegSampleRate(int rate){
        this.posNegSampleRate = rate;
    }

    public  CoNLLDependencyGraph(List<SToken> stokens){
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


    public DependencyRelationship[]  parseDependencyRelationships(Booster edgeTagModel){
        /**
         * figure out dependency using "maximal spanning tree" algorithm.
         * Implemented with a PriorityQuee
         */

        // TODO
        // 发现dependency parsing 存在逻辑错误，待修改； 目前情况：root-x 的edge可能不存在， edge之间相互link
        // priority queue init 的时候不应该塞入全部edge score，而是应该仅仅塞入 0->X1 的所有edge score
        // 依据edge_score 判断X1 
        // 再将 X1-X? 的所有 edge score 放入 priority queue
        // pop 下一个最高的edge(且target_index是还没被cover的)
        // 重复这一动作，直到所有 1-X的index 都被cover
        
        PriorityQueue<ScoreEdge> edgePQ = new PriorityQueue<ScoreEdge>();
        for (int j = 1; j<this.nodeSize; j++){
            edgePQ.add(new ScoreEdge(0,j,this.edgeScores[0][j]));
        }
//        for (int i = 0; i<this.nodeSize; i++){
//            for (int j = 1; j<this.nodeSize; j++){
//                edgePQ.add(new ScoreEdge(i,j,this.edgeScores[i][j]));
//            }
//        }

        // construct a set to keep track of unreached indexes
        HashSet<Integer> unreachedIndexes = new HashSet<Integer>();
        for (int i = 1; i<this.nodeSize; i++) { unreachedIndexes.add(i);};

//        List<DependencyRelationship> relationships = new ArrayList<DependencyRelationship>();

        DependencyRelationship[] relationships = new DependencyRelationship[this.tokens.length-1];

        while (!unreachedIndexes.isEmpty()){
            ScoreEdge selectedEdge = edgePQ.poll();
            if (unreachedIndexes.contains(selectedEdge.target)){

                // 计算tag model需要的特征+模型
//                relationships.add(new DependencyRelationship(selectedEdge.source,selectedEdge.target,this.tokens[selectedEdge.source],this.tokens[selectedEdge.target]));
                relationships[selectedEdge.target-1] = new DependencyRelationship(selectedEdge.source,selectedEdge.target,this.tokens[selectedEdge.source],this.tokens[selectedEdge.target]);
                // 对CoNLLToken进行dependentIndex的更新
                this.tokens[selectedEdge.target].dependentIndex= selectedEdge.source;
                // track 连接到的token
                unreachedIndexes.remove(selectedEdge.target);

                // 基于 selectedEdge.target 添加下一轮的candidate 放入 priority queue

                if (selectedEdge.source==0){
                    edgePQ =  new PriorityQueue<ScoreEdge>();
                }

                for (int j = 1; j<this.nodeSize; j++){
                    edgePQ.add(new ScoreEdge(selectedEdge.target,j,this.edgeScores[selectedEdge.target][j]));
                }

            }
        }

        Float[][] allftrs = this.buildAllTagFtrs();

        try{
            DMatrix dmatrix = new DMatrix(UtilFns.flatten2dFloatArray(allftrs),allftrs.length,allftrs[0].length,Float.NaN);
            float[][] predictScores = edgeTagModel.predict(dmatrix);
            float[] predictScoresFlatten = UtilFns.flatten2dFloatArray(predictScores);
//            System.out.println("score size");
//            System.out.println(predictScores.length);
//            System.out.println("token size");
//            System.out.println(this.tokens.length);
//            System.out.println("relation size");
//            System.out.println(relationships.size());
            for (int i=1 ; i< this.tokens.length; i++){
                this.tokens[i].relationship = float2tag.get(predictScoresFlatten[i-1]);
                relationships[i-1].relationship = float2tag.get(predictScoresFlatten[i-1]);
//                relationships.get(i-1).relationship = float2tag.get(predictScoresFlatten[i-1]);
            }

        }catch(XGBoostError e){
            System.out.println(e);
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


    public Float getLabel(int dependentIndex, int targetIndex){
        float label = this.tokens[targetIndex].getDependentIndex()==dependentIndex ? 1.0f : 0.0f ;
        return label;
    }

    public Float[][] buildAllTagFtrs(){

        LinkedList<int[]> postiveIndexes = new LinkedList<>();
        for (int j = 0; j< this.nodeSize;j++){
            for (int i =0; i< this.nodeSize; i++){
                if (getLabel(i,j)==1.0f) {
                    postiveIndexes.add(new int[]{i, j});
                }
            }
        }
        int ftr_size = buildFtrs(0,0).length;

        Float[][] all_ftrs = new Float[postiveIndexes.size()][ftr_size];
        int count = 0;

//        System.out.println(" ~~~~~~~~ProcessFtr4Sentence~~~~~~~  ");

        for (int[] index : postiveIndexes){
            int i = index[0];
            int j = index[1];
            // 4 debug only' 验证添加特征顺序是否正确
//            System.out.print("ftr dependent index: ");
//            System.out.print(i);
//            System.out.print("  ;;;  ftr target index: ");
//            System.out.println(j);

            all_ftrs[count] = buildFtrs(i,j);
            count+=1;
        }
//        System.out.println(" ~~~~~~~~ProcessFtr4Sentence~~~~~~~  ");
        return all_ftrs;

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

    /**
     * 仅选择跳转 label=1 的 token-pair, 用于训练dependency parsing 具体 tag 的模型
     */
    public void selectTagIndex(){
        this.selectedIndexes = new LinkedList<>();
        for (CoNLLToken token: this.tokens){
            this.selectedIndexes.add(new int[]{token.dependentIndex,token.selfIndex});
        }
    }

    public String[] getAllTagLabel(){
        // 注意root 作为第0个Token没有tag label
        String[] labels = new String[this.tokens.length-1];
//        System.out.print("token size: ");
//        System.out.println(this.tokens.length);

//        System.out.println(" ~~~~~~~~ProcessTag4Sentence~~~~~~~  ");

        for (int i = 1; i < this.tokens.length; i++){
//            System.out.print("token value: ");
//            System.out.print(this.tokens[i].token);
//            System.out.print("  ;;;  token tag: ");
//            System.out.println(this.tokens[i].relationship);
            labels[i-1] = this.tokens[i].relationship;
        }
//        System.out.println(" ~~~~~~~~ProcessTag4Sentence~~~~~~~  ");
        return labels;
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

        System.out.println(UtilFns.toJson(g.tokens));

//        g.selectIndex();
//        System.out.println(UtilFns.toJson(g.selectedIndexes));
//
////        System.out.println(g);
//        System.out.println(g.buildAllFtrs().length);
//        System.out.println(g.buildAllFtrs()[0].length);
//        System.out.println(g.getAllLabel().length);
//
//        System.out.println(g.buildFtrs(0,1).length);
//        System.out.println(Arrays.toString(g.buildFtrs(0,1)));
//        System.out.println(UtilFns.toJson(g.buildAllFtrs()));
//        System.out.println(Arrays.toString(g.getAllLabel()));

//        PriorityQueue<ScoreEdge> pqSE = new PriorityQueue<ScoreEdge>();
//        pqSE.add(new ScoreEdge(0,0,1.0f));
//        pqSE.add(new ScoreEdge(0,0,2.0f));
//        System.out.println(pqSE.poll().score);




    }

}
