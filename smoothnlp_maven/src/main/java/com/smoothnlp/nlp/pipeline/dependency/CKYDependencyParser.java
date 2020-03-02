package com.smoothnlp.nlp.pipeline.dependency;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.basic.UtilFns;
import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import scala.Array;

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
    private static float thresholdRatioStatic = 0.2f;
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

    public DependencyRelationship[] parse(String input) throws XGBoostError {
        List<SToken> tokens = SmoothNLP.POSTAG_PIPELINE.process(input);
        return this.parse(tokens);
    }

    ;

    public DependencyRelationship[] parse(List<SToken> stokens) throws XGBoostError {

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

        float[] nodeSumsFrom = new float[cgraph.size()];
        float[] nodeSumsTo = new float[cgraph.size()];


        for (int i = 0; i < cgraph.size(); i++) {
            for (int j = 0; j < cgraph.size(); j++) {
                edgeScores[i][j] = predictScoresFlatten[i * cgraph.size() + j];
                nodeSumsFrom[i]+=edgeScores[i][j];
                nodeSumsTo[j]+=edgeScores[i][j];
            }
        }

        for (int i = 0; i < cgraph.size(); i++){
            for (int j = 0; j < cgraph.size(); j++){
                // 对两node之间的probability做出normalize
                // "SmoothNLP在V0.3版本中正式推出知识抽取功能" 介词关系正确
                edgeScores[i][j] = edgeScores[i][j]*0.3f
                        +(edgeScores[i][j]/nodeSumsFrom[i])*0.35f
                        +(edgeScores[i][j]/nodeSumsTo[j])*0.35f;
            }
           //For debug, print out score matrix
            System.out.println(" score-"+i+" : "+UtilFns.toJson(edgeScores[i]));
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

        long start = System.currentTimeMillis();
        this.AcedTrees = new HashMap<>();

        ProjectiveTree ptree = new ProjectiveTree(0, cgraph.size()-1, 0);
        ptree.setAllowSourceFromRoot(true);
        ptree.A(edgeScores,this);
        long end = System.currentTimeMillis();
        // evaluate CKY-Algorithm run-time
//        System.out.print("parse PTree time: ");
//        System.out.println(end-start);

//        // print out tree score
//        System.out.println("tree score: "+ptree.score);
//        // print out total tree calculated during optimization for debug
//        System.out.println("tree calcualted total: "+this.AcedTrees.size());
//
//        // print out token size
//        System.out.println("token size: "+stokens.size());
//        // print out total tree calculated during optimization for debug
//        System.out.println("arch size: "+ptree.getArchs().size());
//        System.out.println("ptree score: "+ptree.score);

        List<int[]> arches = ptree.getArchs();
        if (arches.size()!=stokens.size()){
            throw new ValueException("CKY Dependency Parser: tree arches size does not match token size");
        }

        DependencyRelationship[] relationships = new DependencyRelationship[cgraph.size()-1];
        for (int[] arch : arches){
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
//            sum += proba*proba;  // 过度penalize 较小的proba, favor 较大的proba, 结果从(root/动词)出发的edge偏多
            sum+=proba;
//            sum+= Math.sqrt(proba);  // 倾向概率更小的proba
            counter+=1;
        }
        return sum/counter;
    }

    protected class ProjectiveTree {
        int left, right;
        int root,key;
        HashSet<Float> probas;
        float score;
        private ProjectiveTree leftTree, rightTree;
        int[] arch;
        private float thresholdRatio;
        private float thresholdDelta = 0.1f;
        private int greedyCountThreshold = 15;
        HashSet<Integer> reachedIndexes = new HashSet<>();
        private boolean allowSourceFromRoot = false;

        public ProjectiveTree(int left, int right, int root) {
            this.left = left;
            this.right = right;
            this.root = root;
            probas = new HashSet<>();
            this.score = -9999f;
            this.key = key();
            this.autoSetThresholdRatio();
        }

        public void autoSetThresholdRatio(){
            int tokenSize = right-left;
            if (tokenSize<=5) {
                this.thresholdRatio = 1.0f;
            }else if (tokenSize<=10){
                this.thresholdRatio = 0.6f;
            }else if (tokenSize<=20){
                this.thresholdRatio = 0.2f;
            }else if (tokenSize<=30){
                this.thresholdRatio = 0.1f;
            }else if (tokenSize<=40){
                this.thresholdRatio = 0.05f;
            }else{
                this.thresholdRatio = 2.0f/tokenSize;
            }
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

        public void A(float[][] X, CKYDependencyParser ckyParser){

            if (!allowSourceFromRoot){
//                left = Math.max(left,1);
                if (right<left){
                    return;
                }
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
//            if (right - left >= greedyCountThreshold) {  // 对于一定长度以下的tree, 不做greedy处理
//                float sum1= 0;
//                int counter1 = 0;
//                for (int i = left; i <= right; i++){
//                    if  (root!=i){
//                        sum1+=X[root][i];
//                        counter1+=1;
//                    }
//                }
//                archThreshold  = (sum1/counter1)*this.thresholdRatio;
//                if (this.thresholdRatio!=CKYDependencyParser.thresholdRatioStatic){
//                    archThresholdUpperBound = (sum1/counter1)*(this.thresholdRatio+this.thresholdDelta);
//                }
            float[] valid_range = Arrays.copyOfRange(X[root],left,right+1);
            Arrays.sort(valid_range);
            float ratioIndexLower = (right-left)*(1-this.thresholdRatio);
            archThreshold = valid_range[(int) ratioIndexLower];
//            }




            // 一般情况 Dynamic Programming
            for (int j = left; j <= right; j++) {  // other root
                if (j == root) {  // 新root(j)不等于现有root
                    continue;
                }

                if (!this.allowSourceFromRoot & root==0){
                    continue;  // 只允许root有一条outcomming-edge
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
                        if (root == 0 & this.allowSourceFromRoot){ // root 只有一条outcoming-edge
                            tree1 = new ProjectiveTree(1, q, j);
                        }
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

                    try {
                        if (tree1.reachedIndexes.contains(j) | tree2.reachedIndexes.contains(j)) {
                            // 如果tree1/tree2 中 "j"已经被覆盖, 跳过
                            continue;
                        }
                    }catch(Exception e){
                        System.out.println("---- tree1: "+tree1);
                        System.out.println("---- tree2: "+tree2);
                        System.out.println("tree1 indexes: "+tree1.reachedIndexes);
                        System.out.println("tree2indexes: "+tree2.reachedIndexes);
                        throw e;
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

            if (this.score < 0 & right-left >= greedyCountThreshold){  // 如果当前threshold过于严格, 则降低 by this.thresholdDelta
                this.thresholdRatio -= thresholdDelta;
                if (this.thresholdRatio>=0.0f){
                    this.A(X,ckyParser);
                }
//                else{
//                    throw new Exception(archThreshold+" threshold:ratio  "+ this.thresholdRatio +UtilFns.toJson(this));
//                }
            }else{
                this.addSelf2AcedTrees(ckyParser);
                // 记录已经被reached node, 避免重复reach
                if (this.arch!=null){
                    this.reachedIndexes.add(this.arch[1]);
                    this.reachedIndexes.addAll(this.leftTree.reachedIndexes);
                    this.reachedIndexes.addAll(this.rightTree.reachedIndexes);
                }
            }

//            if (right-left!= this.reachedIndexes.size()){
//                System.out.println(this);
//            }

        }

        public List<int[]> getArchs(){
            List<int[]> arches = new LinkedList<>();
            if (this.arch != null){
//                //print out arches for debugging
//                System.out.println(UtilFns.toJson(this.arch)+" : "+UtilFns.toJson(this));
//                System.out.println("      : "+UtilFns.toJson(this.leftTree));
//                System.out.println("      : "+UtilFns.toJson(this.rightTree));
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
//        String text = "一度被政策冷落的油电混合动力汽车，有可能被重新加注鼓励的法码。";
//        String text = "一度被政策冷落的油电混合动力汽车";
//        String text = "韩系轮胎卷入“寒流”";
//        String text = "新款讴歌TL登上环保目录";
//        String text = "呼吸道飞沫传染是新型冠状病毒的主要传播途径";
//        String text = "邯郸市通达机械制造有限公司拥有固定资产1200万元，现有职工280名，其中专业技术人员80名，高级工程师两名，年生产能力10000吨，产值8000万元";
//        String text = "玻璃钢是以合成树脂为基体材料，以玻璃纤维及其制品为增强材料组成的复合材料。";
//        String text = "据了解，三七互娱旗下首款云游戏已在开发当中，未来将登陆华为云游戏平台。";  // 华为postag=vv; 117模型中 "平台"--nn-->"华为"
        String text = "国产特斯拉Model3宣布降价至29.9万元";  // Embed模型中, 特斯拉被识别成VV, 117ftr模型识别正确
//        String text = "上海三维制药有限公司是成立于1958年的中国大型制药企业，现作为上药集团旗下的主要成员之一，专业从事APIs和固体制剂的研究、开发、注册、生产、合同制造、市场推广和销售";
//        String text = "上岛咖啡于1968年进驻于宝岛台湾开始发展";
//        String text = "到目前为止，BMJ是印尼最大、最成功的过滤嘴与卷烟纸供应商，它的产品几乎被应用于每一支印尼生产的机制卷烟中";
//        String text = "公司逐渐成为了质量和产量都居世界领先地位的生产商";
//        String text = "2005年三枪集团实现销售额14亿元人民币，处于行业领先地位";
//        String text = "玻璃钢平板是保温项目普遍采用的外护层材料，具有阻燃，耐腐蚀，同时根据要求配制各种色彩，使外观更美观。";  // 主语问题; 在训练epoch > 1000 后解决
//        String text = "七喜电脑股份有限公司其前身为1997年8月成立的七喜电脑有限公司";
//        String text = "2层口罩的制作材料包括无纺布、鼻梁筋、耳挂。";
//        String text = "SmoothNLP在V0.3版本中正式推出知识抽取功能";
//        String text = "腾讯进军印度保险市场：花15亿元收购一公司10%股份";
//        String text = "中国银行是香港、澳门地区的发钞行";
//        String text =  "中国银行是香港、澳门地区的发钞行，业务范围涵盖商业银行、投资银行、基金、保险、航空租赁等";  // 在137模型中(添加上次词Embedding), 识别: 香港--conj-->地区
//        String text = "口罩是一种卫生用品，一般指戴在口鼻部位用于过滤进入口鼻的空气，以达到阻挡有害的气体、气味、飞沫进出佩戴者口鼻的用具，以纱布或纸等制成。";
//        String text = "Windows API是一套用来控制Windows的各个部件的外观和行为的预先定义的Windows函数";
//        String text = "腾讯云是一家云服务提供商";
        long start = System.currentTimeMillis();
        for (DependencyRelationship e : dparser.parse(text)) {
            System.out.println(e);
        }
        long end = System.currentTimeMillis();
        System.out.print("dependency time: ");
        System.out.println(end-start);

    }
}

