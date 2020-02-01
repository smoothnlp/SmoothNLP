package com.smoothnlp.nlp.pipeline.dependency;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.basic.UtilFns;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


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

    public CKYDependencyParser() {
        init(SmoothNLP.DP_EDGE_SCORE_XGBOOST, SmoothNLP.DP_EDGE_TAG_XGBOOST);
    }

    private void init(String edgeScoreModel, String edgeTagModel) {
        this.edgeScoreModel = UtilFns.loadXgbModel(edgeScoreModel);
        this.edgeTagModel = UtilFns.loadXgbModel(edgeTagModel);
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
        for (int i = 0; i < cgraph.size(); i++) {
            for (int j = 0; j < cgraph.size(); j++) {
//                if (i!=j){  // 过滤一个token 自己依赖自己的情况
                edgeScores[i][j] = predictScoresFlatten[i * cgraph.size() + j];
//                }
            }
            System.out.println(" score-"+i+" : "+UtilFns.toJson(edgeScores[i]));
        }

        System.out.println("tokens: "+UtilFns.toJson(stokens));
        System.out.println("cgraph size: "+cgraph.size());

        long start = System.currentTimeMillis();
        ProjectiveTree ptree = new ProjectiveTree(0, cgraph.size()-1, 0);
        ptree.A(edgeScores);
        long end = System.currentTimeMillis();

//        ProjectiveTree ptree2 = new ProjectiveTree(1, 2, 2);
//        ptree2.A(edgeScores);
//        System.out.println("self tree2: "+UtilFns.toJson(ptree2.arch)+" -- " +UtilFns.toJson(ptree2));

//        System.out.println("self tree: "+UtilFns.toJson(ptree.arch)+" -- " +UtilFns.toJson(ptree));
//        System.out.println("left tree: "+UtilFns.toJson(ptree.leftTree.arch)+" -- " +UtilFns.toJson(ptree.leftTree));
//        System.out.println("right tree: "+UtilFns.toJson(ptree.rightTree.arch)+" -- " +UtilFns.toJson(ptree.rightTree));
//        System.out.println("right left tree: "+UtilFns.toJson(ptree.rightTree.leftTree.arch)+" -- " +UtilFns.toJson(ptree.rightTree.leftTree));
//        System.out.println("right right tree: "+UtilFns.toJson(ptree.rightTree.rightTree.arch)+" -- " +UtilFns.toJson(ptree.rightTree.rightTree));
//
//        System.out.println("right right left tree: "+UtilFns.toJson(ptree.rightTree.rightTree.leftTree.arch)+" -- "+UtilFns.toJson(ptree.rightTree.rightTree.leftTree));
//        System.out.println("right right right tree: "+UtilFns.toJson(ptree.rightTree.rightTree.rightTree.arch)+" -- "+UtilFns.toJson(ptree.rightTree.rightTree.rightTree));

        System.out.println("Arches: "+UtilFns.toJson(ptree.getArchs()));

        System.out.print("parse PTree time: ");
        System.out.println(end-start);

        System.out.println("tree score: "+ptree.score);
        System.out.println("tree probas: "+ptree.probas);

        System.out.println("tree calcualted total: "+ptree.AcedTrees.size());

        return null;
    }

    ;

    public static float computeScore(List<Float> probas) {
        float logSum = 0f;
        for (float  proba : probas){
//            System.out.println(proba +" add score: "+Math.log(proba));
            logSum+=Math.log(proba);
//            logSum+=proba;
        }
        return logSum/probas.size();
    }



    protected class ProjectiveTree {
        int left, right;
        int root,key;
        List<Float> probas;
        float score;
        ProjectiveTree leftTree, rightTree;
        float[] arch;
        HashMap<Integer,ProjectiveTree> AcedTrees;

        public ProjectiveTree(int left, int right, int root) {
            this.left = left;
            this.right = right;
            this.root = root;
            probas = new LinkedList<>();
            this.score = -9999f;
            this.AcedTrees = new HashMap<>();
            this.key = key();
        }

        public ProjectiveTree(int left, int right, int root,  HashMap<Integer,ProjectiveTree> AcedTrees){
            this.left = left;
            this.right = right;
            this.root = root;
            probas = new LinkedList<>();
            this.score = -9999f;
            this.AcedTrees = AcedTrees;
            this.key = key();
        }

        public int key(){
            return this.root*10000+this.left*100+this.right;
        }

        public void addSelf2AcedTrees(){
            this.AcedTrees.put(this.key, this);
        }

        public boolean checkAlreadyAcedATree(ProjectiveTree tree){
            return this.AcedTrees.containsKey(tree.key);
        }

        public ProjectiveTree getAcedATree(ProjectiveTree tree){
            return this.AcedTrees.get(tree.key);
        }

        public void A(float[][] X) {
            // Dynamic Programming Stop Conditon
            // 动态规划的停止条件

            if (left == right){
                if (root==left){
//                    System.err.println(" -- Invalid pt tree --");
                    return;
                }else{
                    this.probas.add(X[root][left]);
                    this.arch = new float[]{root,left};
                    this.score = computeScore(this.probas);
                    this.addSelf2AcedTrees();
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
                    this.arch = new float[]{root,target};
                    this.score = computeScore(this.probas);
                    this.addSelf2AcedTrees();
                    return;
                }
            }

            // 一般情况 Dynamic Programming
            for (int j = left; j <= right; j++) {  // other root
                if (j == root) {
                    continue;
                }
                for (int q = left; q < right; q += 1) {
                    ProjectiveTree tree1, tree2;
                    if (j > root) {
                        tree1 = new ProjectiveTree(left, q, root,this.AcedTrees);
                        tree2 = new ProjectiveTree(q + 1, right, j,this.AcedTrees);
                    } else { // j<root
                        tree1 = new ProjectiveTree(left, q, j,this.AcedTrees);
                        tree2 = new ProjectiveTree(q + 1, right, root,this.AcedTrees);
                    }

                    List<Float> _probas = new LinkedList<>();
                    if (this.checkAlreadyAcedATree(tree1)){
                        tree1 = this.getAcedATree(tree1);
                    }else{
                        tree1.A(X);
                    }

                    if (this.checkAlreadyAcedATree(tree2)){
                        tree2 = this.getAcedATree(tree2);
                    }else{
                        tree2.A(X);
                    }
//                    tree1.A(X);
//                    tree2.A(X);
                    tree1.addSelf2AcedTrees();
                    tree2.addSelf2AcedTrees();
                    _probas.addAll(tree1.probas);
                    _probas.addAll(tree2.probas);

                    this.AcedTrees.putAll(tree1.AcedTrees);
                    this.AcedTrees.putAll(tree2.AcedTrees);

//                    System.out.println(this.AcedTrees.size());


                    _probas.add(X[root][j]);

                    float _score = computeScore(_probas);

                    if (_score > this.score) {
                        this.score = _score;
                        this.probas = _probas;
                        this.leftTree = tree1;
                        this.rightTree = tree2;
                        this.arch = new float[]{root,j};
                    }
                }
            }
        }

        public List<float[]> getArchs(){
            List<float[]> arches = new LinkedList<>();
            if (this.arch != null){
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
        List<SToken> tokens = SmoothNLP.POSTAG_PIPELINE.process("阿里巴巴由马云与蔡崇信在杭州成立");
        long start = System.currentTimeMillis();
        dparser.parse(tokens);
//        for (DependencyRelationship e : dparser.parse("阿里巴巴成立")) {
//            System.out.println(e);
//        }
        long end = System.currentTimeMillis();
        System.out.print("dependency time: ");
        System.out.println(end-start);

    }
}
