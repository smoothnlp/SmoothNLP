package com.smoothnlp.nlp.pipeline.dependency;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.basic.UtilFns;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;

import java.util.*;


/**
 * Implmentation of CKY Projective Dependency Tree Parsing Algorithm;
 * reference: Dependency Parsing Synthesis Lectures on Human Language Technologies.pdf Pg.51
 */
public class CKYDependencyParser implements IDependencyParser {

    /**
     * Cocke–Younger–Kasami algorithm
     * Keyword: bottom-up parsing and dynamic programming.
     */

    private Booster edgeScoreModel;
    private Booster edgeTagModel;

    HashMap<Integer,ProjectiveTree> AcedTrees;
    private static float thresholdRatioStatic = 1.2f;

    public CKYDependencyParser() {
        init(SmoothNLP.DP_EDGE_SCORE_XGBOOST, SmoothNLP.DP_EDGE_TAG_XGBOOST);
    }

    private void init(String edgeScoreModel, String edgeTagModel) {
        this.edgeScoreModel = UtilFns.loadXgbModel(edgeScoreModel);
        this.edgeTagModel = UtilFns.loadXgbModel(edgeTagModel);
        this.AcedTrees = new HashMap<>();
    }

    public DependencyRelationship[] parse(String input) throws Exception {
        List<SToken> tokens = SmoothNLP.POSTAG_PIPELINE.process(input);
        return this.parse(tokens);
    }

    ;

    public DependencyRelationship[] parse(List<SToken> stokens) throws Exception {

        CoNLLDependencyGraph cgraph = new CoNLLDependencyGraph(stokens);
        // build ftrs
        Float[][] pairFtrs = cgraph.buildAllFtrs();
        float[] flattenPairFtrs = UtilFns.flatten2dFloatArray(pairFtrs);

        int numRecords = pairFtrs.length;
        int numFtrs = pairFtrs[0].length;
        DMatrix dmatrix = new DMatrix(flattenPairFtrs, numRecords, numFtrs);

        float[][] predictScores = this.edgeScoreModel.predict(dmatrix, false, SmoothNLP.XGBoost_DP_Edge_Model_Predict_Tree_Limit);  // 调节treeLimit , 优化时间

        float[] predictScoresFlatten = UtilFns.flatten2dFloatArray(predictScores);
        float[][] edgeScores = new float[cgraph.size()][cgraph.size()];

        float[] nodeSums = new float[cgraph.size()];

        for (int i = 0; i < cgraph.size(); i++) {
            for (int j = 0; j < cgraph.size(); j++) {
                edgeScores[i][j] = predictScoresFlatten[i * cgraph.size() + j];
                nodeSums[j]+=edgeScores[i][j];
            }
            System.out.println(" score-"+i+" : "+UtilFns.toJson(edgeScores[i]));
        }

        System.out.println("tokens: "+UtilFns.toJson(stokens));
        System.out.println("cgraph size: "+cgraph.size());

        long start = System.currentTimeMillis();
        this.AcedTrees = new HashMap<>();
        ProjectiveTree ptree = new ProjectiveTree(0, cgraph.size()-1, 0);
        ptree.A(edgeScores,this);
        long end = System.currentTimeMillis();

        System.out.println("Arches: "+UtilFns.toJson(ptree.getArchs()));
        System.out.println("Arch size: "+UtilFns.toJson(ptree.getArchs().size()));

        System.out.print("parse PTree time: ");
        System.out.println(end-start);

        System.out.println("tree score: "+ptree.score);
        System.out.println("tree probas: "+ptree.probas);

        System.out.println("tree calcualted total: "+this.AcedTrees.size());

        DependencyRelationship[] relationships = new DependencyRelationship[cgraph.size()-1];
        for (int[] arch :  ptree.getArchs()){
            DependencyRelationship rel = new DependencyRelationship(arch[0],arch[1],cgraph.tokens[arch[0]],cgraph.tokens[arch[1]]);
            rel._edge_score = edgeScores[arch[0]][arch[1]];
            cgraph.tokens[arch[1]].dependentIndex = arch[0];
            relationships[arch[1]-1] = rel;
        }

        // 计算Relationship的tag
        Float[][] allftrs = cgraph.buildAllTagFtrs();
        System.out.println(" all tag feture size: "+allftrs.length);
        dmatrix = new DMatrix(UtilFns.flatten2dFloatArray(allftrs),allftrs.length,allftrs[0].length,Float.NaN);
        float[][] predictprobas = edgeTagModel.predict(dmatrix,false,SmoothNLP.XGBoost_DP_tag_Model_Predict_Tree_Limit);
        for (int i=1 ; i< cgraph.tokens.length; i++){
            float[] probas = predictprobas[i-1];
            int max_index = 0;
            for (int index =0; index<probas.length; index+=1 ){
                if (probas[index] > probas[max_index]){
                    max_index = index;
                }
            }
            cgraph.tokens[i].relationship = cgraph.float2tag.get((float) max_index);
            relationships[i-1].relationship = cgraph.float2tag.get((float) max_index);
            relationships[i-1]._tag_score = predictprobas[i-1][max_index];
       }


        return relationships;
    }

    ;

    public static float computeScore(Iterable<Float> probas) {
        float sum = 0f;
        int counter = 0;
        for (float  proba : probas){
//            sum+=Math.log(proba)+2;
            sum+=proba;
            counter+=1;
        }
        return sum/counter;
    }



    protected class ProjectiveTree {
        int left, right;
        int root,key;
        HashSet<Float> probas;
        float score;
        ProjectiveTree leftTree, rightTree;
        int[] arch;
        private float thresholdRatio = CKYDependencyParser.thresholdRatioStatic;
        private float thresholdDelta = 0.1f;
//        HashSet<Integer> reachedIndexes = new HashSet<>();

        public ProjectiveTree(int left, int right, int root) {
            this.left = left;
            this.right = right;
            this.root = root;
            probas = new HashSet<>();
            this.score = -9999f;
            this.key = key();
        }

        public int key(){
            return this.root*10000+this.left*100+this.right;
        }

        public void addSelf2AcedTrees(CKYDependencyParser ckyParser){
            ckyParser.AcedTrees.put(this.key, this);
        }

        public boolean checkAlreadyAcedATree(CKYDependencyParser ckyParser,ProjectiveTree tree){
            return ckyParser.AcedTrees.containsKey(tree.key);
        }

        public ProjectiveTree getAcedATree(CKYDependencyParser ckyParser,ProjectiveTree tree){
            return ckyParser.AcedTrees.get(tree.key);
        }

        public void A(float[][] X, CKYDependencyParser ckyParser) throws Exception{
            // Dynamic Programming Stop Conditon
            // 动态规划的停止条件
            if (left == right){
                if (root==left){  // left, right, root 不能同时相等
//                    System.err.println(" -- Invalid pt tree --");
                    return;
                }else{
                    this.probas.add(X[root][left]);
                    this.arch = new int[]{root,left};
                    this.score = computeScore(this.probas);
                    this.addSelf2AcedTrees(ckyParser);
                    return;
                }
            }

            if (right-left==1){
                int target = -1;
                if (root==left){
                    target = right;
                }
                if (root == right){
                    target = left;
                }
                if (target >= 0 ){
                    this.probas.add(X[root][target]);
                    this.arch = new int[]{root,target};
                    this.score = computeScore(this.probas);
                    this.addSelf2AcedTrees(ckyParser);
                    return;
                }
            }

            // 计算gready算法所需要的log
            float archThreshold = 0f;
            float archThresholdUpperBound = 1.0f;
            if (right - left >= 8) {  // 对于一定长度以下的tree, 不做greedy处理
                float sum1= 0;
                int counter1 = 0;
                for (int i = left; i <= right; i++){
                    if  (root!=i){
                        sum1+=X[root][i];
                        counter1+=1;
                    }
                }
                archThreshold  = (sum1/counter1)*this.thresholdRatio;
                if (this.thresholdRatio!=CKYDependencyParser.thresholdRatioStatic){
                    archThresholdUpperBound = (sum1/counter1)*(this.thresholdRatio+this.thresholdDelta);
                }
            }



            // 一般情况 Dynamic Programming
            for (int j = left; j <= right; j++) {  // other root
                if (j == root) {  // 新root(j)不等于现有root
                    continue;
                }

                if ( X[root][j] > archThresholdUpperBound | X[root][j] < archThreshold){
                    // greedy 的部分, 对于candidate arch X[root][j] 不在阈值范围内, 则跳过
                    continue;
                }

                for (int q = left; q < right; q += 1) {
                    ProjectiveTree tree1, tree2;
                    if (j > root) {
                        if(j<q){
                            continue;
                        }
                        tree1 = new ProjectiveTree(left, q, root);
                        tree2 = new ProjectiveTree(q + 1, right, j);
                    } else { // j<root
                        if(j>=q+1 | j==right){
                            continue;
                        }
                        tree1 = new ProjectiveTree(left, q, j);
                        tree2 = new ProjectiveTree(q + 1, right, root);
                    }

                    HashSet<Float> _probas = new HashSet<>();
                    if (this.checkAlreadyAcedATree(ckyParser,tree1)){
                        tree1 = this.getAcedATree(ckyParser,tree1);
                    }else{
                        tree1.A(X,ckyParser);
                        tree1.addSelf2AcedTrees(ckyParser);
                    }

                    if (this.checkAlreadyAcedATree(ckyParser,tree2)){
                        tree2 = this.getAcedATree(ckyParser,tree2);
                    }else{
                        tree2.A(X,ckyParser);
                        tree2.addSelf2AcedTrees(ckyParser);
                    }

                    _probas.addAll(tree1.probas);
                    _probas.addAll(tree2.probas);
                    _probas.add(X[root][j]);

                    float _score = computeScore(_probas);

                    if (_score > this.score) {
                        this.score = _score;
                        this.probas = _probas;
                        this.leftTree = tree1;
                        this.rightTree = tree2;
                        this.arch = new int[]{root,j};  // 新arch为现有root到新root(j)
                    }
                }
            }

            if (this.score < 0){
                this.thresholdRatio -= thresholdDelta;
                if (this.thresholdRatio>=0.0f){
                    this.A(X,ckyParser);
                } else{
                    throw new Exception(archThreshold+" threshold:ratio  "+ this.thresholdRatio +UtilFns.toJson(this));
                }
            }
            else{
                this.addSelf2AcedTrees(ckyParser);
            }

        }

        public List<int[]> getArchs(){
            List<int[]> arches = new LinkedList<>();
            if (this.arch != null){
                System.out.println(UtilFns.toJson(this.arch)+" : "+UtilFns.toJson(this));
                arches.add(this.arch);
            }
            if (this.leftTree != null){
                arches.addAll(this.leftTree.getArchs());
            }
            if (this.rightTree != null){
                arches.addAll(this.rightTree.getArchs());
            }
            return arches;
        }


    }

    public static void main(String[] args) throws Exception {
        IDependencyParser dparser = new CKYDependencyParser();
        String text = "在面对用户的搜索产品不断丰富的同时，百度还创新性地推出了基于搜索的营销推广服务，并成为最受企业青睐的互联网营销推广平台。";
//        String text = "阿里巴巴与腾讯达成合作协议";
        long start = System.currentTimeMillis();
        for (DependencyRelationship e : dparser.parse(text)) {
            System.out.println(e);
        }
        long end = System.currentTimeMillis();
        System.out.print("dependency time: ");
        System.out.println(end-start);

    }
}

