package com.smoothnlp.nlp.pipeline.dependency;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.basic.UtilFns;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;

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
    private static float thresholdRatioStatic = 1.0f;
    public float[] targetAvgProbas;

    public CKYDependencyParser() {
        init(SmoothNLP.DP_EDGE_SCORE_XGBOOST, SmoothNLP.DP_EDGE_TAG_XGBOOST);
    }

    public void setThresholdRatioStatic(float thresholdRatioStatic) {
        this.thresholdRatioStatic = thresholdRatioStatic;
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
            // For debug, print out score matrix
//            System.out.println(" score-"+i+" : "+UtilFns.toJson(edgeScores[i]));
        }

        this.targetAvgProbas = new float[cgraph.size()];
        for (int j = 0; j < cgraph.size(); j++) {
            float targetSum = 0f;
            for (int i = 0; i < cgraph.size(); i++) {
                if (i!=j){
                    targetSum+=edgeScores[i][j];
                }
            }
            this.targetAvgProbas[j]=targetSum/(cgraph.size()-1);
        }

//        long start = System.currentTimeMillis();
        this.AcedTrees = new HashMap<>();
        ProjectiveTree ptree = new ProjectiveTree(0, cgraph.size()-1, 0);
        ptree.setAllowSourceFromRoot(true);
        ptree.A(edgeScores,this);
//        long end = System.currentTimeMillis();
//        // evaluate CKY-Algorithm run-time
//        System.out.print("parse PTree time: ");
//        System.out.println(end-start);

//        // print out tree score
//        System.out.println("tree score: "+ptree.score);
//        // print out total tree calculated during optimization for debug
//        System.out.println("tree calcualted total: "+this.AcedTrees.size());

        DependencyRelationship[] relationships = new DependencyRelationship[cgraph.size()-1];
        for (int[] arch :  ptree.getArchs()){
            DependencyRelationship rel = new DependencyRelationship(arch[0],arch[1],cgraph.tokens[arch[0]],cgraph.tokens[arch[1]]);
            rel._edge_score = edgeScores[arch[0]][arch[1]];
            cgraph.tokens[arch[1]].dependentIndex = arch[0];
            relationships[arch[1]-1] = rel;
        }

        // 计算Relationship的tag
        Float[][] allftrs = cgraph.buildAllTagFtrs();
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
            cgraph.tokens[i].relationship = cgraph.float2tag.get((float) max_index);  // update cgraph object
            relationships[i-1].relationship = cgraph.float2tag.get((float) max_index);
            relationships[i-1]._tag_score = predictprobas[i-1][max_index];
            if (cgraph.tokens[i].dependentIndex==0){  // 从root出发的relationship, tag强制为"root"
                cgraph.tokens[i].relationship = "root";
                relationships[i-1].relationship ="root";
                relationships[i-1]._tag_score = 1.0f;
            }
       }


        return relationships;
    }

    /**
     * 计算每个Projective Tree 的 evaluation score
     * @param probas
     * @return
     */
    public static float computeScore(Iterable<Float> probas) {

        float sum = 0f;
        int counter = 0;
        for (float  proba : probas){
//            sum+=Math.log(proba)+2; // 计算时间过长
            sum+=proba*proba;
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
        private float thresholdDelta = 0.05f;
        HashSet<Integer> reachedIndexes = new HashSet<>();
        private boolean allowSourceFromRoot = false;

        public ProjectiveTree(int left, int right, int root) {
            this.left = left;
            this.right = right;
            this.root = root;
            probas = new HashSet<>();
            this.score = -9999f;
            this.key = key();
        }

        public void setAllowSourceFromRoot(boolean allowSourceFromRoot) {
            this.allowSourceFromRoot = allowSourceFromRoot;
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

            if (!this.allowSourceFromRoot & root==0){
                return;  // 只允许root有一条outcomming-edge
            }

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
                    this.reachedIndexes.add(left);
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
                    this.reachedIndexes.add(target);
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

                if (X[root][j] < ckyParser.targetAvgProbas[j]*this.thresholdRatio-this.thresholdDelta){
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
                    if (this.checkAlreadyAcedATree(ckyParser,tree1)){  // 如果 tree1 已经被计算, 从cache中取出定义object
                        tree1 = this.getAcedATree(ckyParser,tree1);
                    }else {
                        tree1.A(X, ckyParser);
                        tree1.addSelf2AcedTrees(ckyParser);
                    }

                    if (this.checkAlreadyAcedATree(ckyParser,tree2)){
                        tree2 = this.getAcedATree(ckyParser,tree2);
                    }else{
                        tree2.A(X,ckyParser);
                        tree2.addSelf2AcedTrees(ckyParser);
                    }

                    if (tree1.reachedIndexes.contains(j) | tree2.reachedIndexes.contains(j)){
                        // 如果tree1/tree2 中 "j"已经被覆盖, 跳过
                        continue;
                    }

                    _probas.addAll(tree1.probas);
                    _probas.addAll(tree2.probas);
                    _probas.add(X[root][j]);

                    float _score = computeScore(_probas);  // 计算score

                    if (_score > this.score) {
                        this.score = _score;
                        this.probas = _probas;
                        this.leftTree = tree1;
                        this.rightTree = tree2;
                        this.arch = new int[]{root,j};  // 新arch为现有root到新root(j)
                    }
                }
            }

            if (this.score < 0){  // 如果当前threshold过于严格, 则降低 by this.thresholdDelta
                this.thresholdRatio -= thresholdDelta;
                if (this.thresholdRatio>=0.0f){
                    this.A(X,ckyParser);
                } else{
                    throw new Exception(archThreshold+" threshold:ratio  "+ this.thresholdRatio +UtilFns.toJson(this));
                }
            }
            else{
                this.addSelf2AcedTrees(ckyParser);
                // 记录已经被reached node, 避免重复reach
                this.reachedIndexes.add(this.arch[1]);
                this.reachedIndexes.addAll(this.leftTree.reachedIndexes);
                this.reachedIndexes.addAll(this.rightTree.reachedIndexes);
            }

        }

        public List<int[]> getArchs(){
            List<int[]> arches = new LinkedList<>();
            if (this.arch != null){
                // print out arches for debugging
//                System.out.println(UtilFns.toJson(this.arch)+" : "+UtilFns.toJson(this));
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
//        String text = "在面对用户的搜索产品不断丰富的同时，百度还创新性地推出了基于搜索的营销推广服务，并成为最受企业青睐的互联网营销推广平台。";
//        String text = "百度还创新性地推出了基于搜索的营销推广服务";
        String text = "一度被政策冷落的油电混合动力汽车，有可能被重新加注鼓励的法码。";
        long start = System.currentTimeMillis();
        for (DependencyRelationship e : dparser.parse(text)) {
            System.out.println(e);
        }
        long end = System.currentTimeMillis();
        System.out.print("dependency time: ");
        System.out.println(end-start);

    }
}

