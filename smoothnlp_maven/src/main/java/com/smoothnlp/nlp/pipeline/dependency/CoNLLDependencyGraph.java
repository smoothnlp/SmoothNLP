package com.smoothnlp.nlp.pipeline.dependency;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.CoNLLToken;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.basic.UtilFns;

import java.util.*;

import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import ml.dmlc.xgboost4j.java.Booster;

public class CoNLLDependencyGraph {

    /**
     * construct dependencies as a graph
     */

    public CoNLLToken[] tokens;
    private int nodeSize;
    public float[][] edgeScores;
    private int posNegSampleRate = 2;
    private LinkedList<int[]> selectedIndexes = null;
    private float[][] tagProba;

    private static float FloatRange = 1l << 32;

    private static Map<String, Integer> postag2index;
    static {
        postag2index = new HashMap<>();
        postag2index.put("AD",0);
        postag2index.put("AS",1);
        postag2index.put("BA",0);
        postag2index.put("CC",2);
        postag2index.put("CD",3);
        postag2index.put("CS",0);
        postag2index.put("DEC",4);
        postag2index.put("DEG",4);
        postag2index.put("DER",4);
        postag2index.put("DEV",4);
        postag2index.put("DT",5);
        postag2index.put("ETC",0);
        postag2index.put("FRAG",0);
        postag2index.put("FW",0);
        postag2index.put("IJ",6);
        postag2index.put("JJ",7);
        postag2index.put("LB",0);
        postag2index.put("LC",8);
        postag2index.put("M",9);
        postag2index.put("MSP",0);
        postag2index.put("NN",10);
        postag2index.put("NOI",0);
        postag2index.put("NR",11);
        postag2index.put("NT",12);
        postag2index.put("OD",0);
        postag2index.put("ON",0);
        postag2index.put("P",13);
        postag2index.put("PN",14);
        postag2index.put("PU",15);
        postag2index.put("SB",0);
        postag2index.put("SP",16);
        postag2index.put("URL",0);
        postag2index.put("VA",17);
        postag2index.put("VC",18);
        postag2index.put("VE",19);
        postag2index.put("VV",20);
        postag2index.put("LOC",21);
        postag2index.put("CTY",22);
        postag2index.put("DTR",23);
        postag2index.put("DTA",24);
    }

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
        float2tag.put(46.0f, "plmod");
    }

    public static float cosineSimilarity(float[] vectorA, float[] vectorB) {
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public CoNLLDependencyGraph(CoNLLToken[] tokens){
        this.tokens = tokens;
        this.nodeSize = this.tokens.length;
        this.tagProba = SmoothNLP.POSTAG_PIPELINE.getAllProba(this.tokens);
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
        this.tagProba = SmoothNLP.POSTAG_PIPELINE.getAllProba(this.tokens);
    }

    public String toString(){
        return UtilFns.join("\n",this.tokens);
    }

    public void setEdgeScores(float[][] edgeScores){
        this.edgeScores = edgeScores;
    }


    public boolean check2edgeCrossed(DependencyRelationship rel1, DependencyRelationship rel2){
        int rel1_start = Math.min(rel1.targetIndex,rel1.dependentIndex);
        int rel1_end = Math.max(rel1.targetIndex,rel1.dependentIndex);
        int rel2_start = Math.min(rel2.targetIndex,rel2.dependentIndex);
        int rel2_end = Math.max(rel2.targetIndex,rel2.dependentIndex);
        int range1 = rel1_end-rel1_start;
        int range2 = rel2_end - rel2_start;
        Set<Integer> covered_range = new HashSet<>();
        for (int i = rel1_start; i < rel1_end;i++){covered_range.add(i);}
        for (int i = rel2_start; i < rel2_end;i++){covered_range.add(i);}
        if (covered_range.size()==Math.max(range1,range2)){
            return false;
        }
        if (covered_range.size()==range1+range2){
            return false;
        }
        return true;
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

        // construct a set to keep track of unreached indexes
        HashSet<Integer> unreachedIndexes = new HashSet<Integer>();

        for (int i = 1; i<this.nodeSize; i++) { unreachedIndexes.add(i);};

        DependencyRelationship[] relationships = new DependencyRelationship[this.tokens.length-1];
        List<DependencyRelationship> relationships_list = new LinkedList<>();

        List<ScoreEdge> passedonEdges = new LinkedList<>();

        while (!unreachedIndexes.isEmpty() & !edgePQ.isEmpty()){  // | !edgePQ.isEmpty()
            ScoreEdge selectedEdge = edgePQ.poll();  // 不断从pq中抽出 candidate edge
            if (selectedEdge==null){
                continue;
            }

            if (unreachedIndexes.isEmpty() & !passedonEdges.isEmpty()){
                edgePQ.addAll(passedonEdges);
                passedonEdges =  new LinkedList<>();
            }

            try{
                if (unreachedIndexes.isEmpty() & selectedEdge.score<0.3){
                    continue;
                }
            }catch(Exception e){
                System.out.println(unreachedIndexes);
                System.out.println(selectedEdge);
                throw e;
            }


            if (unreachedIndexes.contains(selectedEdge.target) | unreachedIndexes.isEmpty() ){
                boolean projectable = true;

                // 检查是否有edge交叉的情况
                DependencyRelationship candidate_rel = new DependencyRelationship(selectedEdge.source,selectedEdge.target,this.tokens[selectedEdge.source],this.tokens[selectedEdge.target]);
                for (DependencyRelationship rel : relationships){
                    if (rel != null){
                        if (this.check2edgeCrossed(rel,candidate_rel)){
                            projectable = false;
                            break;
                        }
                    }
                }

                if (!projectable){  // 存在交叉的情况, skip 这条edge
                    continue;
                }



                // 计算tag model需要的特征+模型
                // construct edge, 放入 array中

                DependencyRelationship new_rel = candidate_rel;
                new_rel._edge_score = selectedEdge.score;


                relationships_list.add(new_rel);


                if (!unreachedIndexes.isEmpty()){

                    relationships[selectedEdge.target-1] = new_rel;

                    // 对CoNLLToken进行dependentIndex的更新
                    this.tokens[selectedEdge.target].dependentIndex= selectedEdge.source;

                    // track 连接到的token
                    unreachedIndexes.remove(selectedEdge.target);

                    // 基于 selectedEdge.target 添加下一轮的candidate 放入 priority queue

                    if (selectedEdge.source==0){  // 第一轮root 找到后, 清空 priority queue
                        edgePQ =  new PriorityQueue<ScoreEdge>();
//                        for (int i = 1; i< this.nodeSize;i+=1){
//                            for (int j = 1; j<this.nodeSize; j++){
//                                if (i!=j){
//                                    edgePQ.add(new ScoreEdge(i,j,this.edgeScores[i][j]));
//                                }
//
//                            }
//                        }
                    }

                    // 根据最新插入的edge, 加入以新token为dependent 的 edge
                    int start = 1;
                    int end = this.nodeSize;
                    for (int i = 1; i < selectedEdge.target; i++){
                        if (relationships[i-1] != null){
                            start = i+1;
                        }
                    }

                    for (int i = this.nodeSize-1 ; i > selectedEdge.target; i--){
                        if (relationships[i-1] != null){
                            end = i;
                        }
                    }


                    for (int j = start; j<end; j++){  // 损失projectivity, 允许现有的edge 到任何 edge
                        edgePQ.add(new ScoreEdge(selectedEdge.target,j,this.edgeScores[selectedEdge.target][j]));
                    }
                }

            }else if (!unreachedIndexes.isEmpty()){
                passedonEdges.add(selectedEdge);  // 如果dp_tree 尚未 full-span, 且该edge invalid, 暂时存起来
            }
        }

        try{
            // 处理 depedency_relaitonship Tree

            Float[][] allftrs = this.buildAllTagFtrs();
            DMatrix dmatrix = new DMatrix(UtilFns.flatten2dFloatArray(allftrs),allftrs.length,allftrs[0].length,Float.NaN);

            float[][] predictprobas = edgeTagModel.predict(dmatrix,false,SmoothNLP.XGBoost_DP_tag_Model_Predict_Tree_Limit);

//            System.out.println(predictprobas.length);
//            System.out.println(predictprobas[0].length);

//            float[][] predictScores = edgeTagModel.predict(dmatrix);
//            float[] predictScoresFlatten = UtilFns.flatten2dFloatArray(predictScores);
            for (int i=1 ; i< this.tokens.length; i++){

                float[] probas = predictprobas[i-1];
                int max_index = 0;
                for (int index =0; index<probas.length; index+=1 ){
                    if (probas[index] > probas[max_index]){
                        max_index = index;
                    }
                }


                this.tokens[i].relationship = float2tag.get((float) max_index);
                relationships[i-1].relationship = float2tag.get((float) max_index);
                relationships[i-1]._tag_score = predictprobas[i-1][max_index];
//                System.out.println(UtilFns.toJson(relationships[i-1]));
//                relationships.get(i-1).relationship = float2tag.get(predictScoresFlatten[i-1]);
            }

            // -------------- 处理 depedency_relaitonship Graph --------------
//            List<int[]> selected_index_pair = new LinkedList<>();
//            for (DependencyRelationship rel : relationships_list){
//                selected_index_pair.add(new int[]{rel.dependentIndex,rel.targetIndex});
//            }
//
//            int ftr_size = buildFtrs(0,0).length;
//            allftrs = new Float[relationships_list.size()][ftr_size];
//            int count = 0;
//            for (int[] index : selected_index_pair){
//                int i = index[0];
//                int j = index[1];
//                allftrs[count] = buildFtrs(i,j);
//                count+=1;
//            }
//
//            dmatrix = new DMatrix(UtilFns.flatten2dFloatArray(allftrs),allftrs.length,allftrs[0].length,Float.NaN);
//            predictprobas = edgeTagModel.predict(dmatrix,false);
//            count = 0;
//            for (DependencyRelationship rel: relationships_list){
//                float[] probas = predictprobas[count];
//                int max_index = 0;
//                for (int index =0; index<probas.length; index+=1 ){
//                    if (probas[index] > probas[max_index]){
//                        max_index = index;
//                    }
//                }
//                rel.relationship = float2tag.get((float)max_index);
//                rel._tag_score = predictprobas[count][max_index];
//                count+=1;
//            }
            // -------------- 处理 depedency_relaitonship Graph --------------

        }catch(XGBoostError e){
            System.out.println(e);
        }

        return relationships;
//        return relationships_list.toArray(new DependencyRelationship[relationships_list.size()]);
    }


    public int size(){
        return this.nodeSize;
    }

    public static float hashString(String text){
//        return text.hashCode();
        String sHash = "0."+Math.abs(text.hashCode());
        return Float.valueOf(sHash);
    }

    public Float[] buildFtrs(int dependentIndex, int targetIndex){
        return buildFtrs(dependentIndex,targetIndex,true, true);
    }

    public Float[] buildFtrs(int dependentIndex, int targetIndex,
                             boolean withTokenPosition, boolean withNeighborVec){
        /**
         * the feature building might be simple in early stages
         * for latter development, please reference: https://github.com/orgs/smoothnlp/teams/let-s-survive/discussions/6
         */
        List<Float> ftrs = new LinkedList<>();
        // 两个token 的 hashcode
//        float dhashcode = hashString(this.tokens[dependentIndex].getToken());
//        float thashcode = hashString(this.tokens[targetIndex].getToken());
//        ftrs.add(dhashcode);
//        ftrs.add(thashcode);

        // 两个postag 的 hashcode
//        float dpostag_hcode = this.tokens[dependentIndex].getPostag().hashCode();
//        float tpostag_hcode = this.tokens[targetIndex].getPostag().hashCode();
//        float dpostag_hcode = hashString(this.tokens[dependentIndex].getPostag());
//        float tpostag_hcode = hashString(this.tokens[targetIndex].getPostag());
//        ftrs.add(dpostag_hcode);
//        ftrs.add(tpostag_hcode);

        ftrs.add(this.tokens[dependentIndex].getTagproba());
        ftrs.add(this.tokens[targetIndex].getTagproba());

        if (withTokenPosition){
            // 特征之间的 位置差
//        ftrs.add((float)dependentIndex - targetIndex); // 去除token之间差距绝对值作为特征
            ftrs.add(((float)dependentIndex - targetIndex)/this.tokens.length);

            // 两个token本身在句子中的位置
            ftrs.add(((float)dependentIndex)/this.tokens.length);
            ftrs.add(((float)targetIndex)/this.tokens.length);
        }


        // 添加 dependent 与 target 词邻近词的 postag
        for (int index: new int[]{dependentIndex,targetIndex}){
            for (int shift: new int[]{1,2,3}){
                int left_neighbor_index = index-shift;
                int right_neighbor_index = index+shift;

                float leftFtr = 0.0f;
                if (left_neighbor_index<0){
                    leftFtr = hashString("start");
                }else{
                    leftFtr = hashString(this.tokens[left_neighbor_index].getPostag());
                }
                ftrs.add(leftFtr);
                float rightFtr = 0.0f;
                if (right_neighbor_index<this.tokens.length){
                    rightFtr = hashString(this.tokens[right_neighbor_index].getPostag());
                }else{
                    rightFtr = hashString("end");
                }
                ftrs.add(rightFtr);
            }
        }

        if (withNeighborVec){
            int left_shift = Math.max(0,targetIndex-1);
            int right_shift = Math.min(targetIndex+1,this.tokens.length-1);
            float[] leftneigbor_vec = SmoothNLP.WORDEMBEDDING_PIPELINE.processToken(this.tokens[left_shift]);
            float[] rightneigbor_vec = SmoothNLP.WORDEMBEDDING_PIPELINE.processToken(this.tokens[right_shift]);
            for (float f: leftneigbor_vec) {ftrs.add(f);}
            for (float f: rightneigbor_vec) {ftrs.add(f);}

//            // add neighbor cosine similarity
//            ftrs.add(cosineSimilarity(leftneigbor_vec,rightneigbor_vec));

//            float[] neighbor_vec = SmoothNLP.WORDEMBEDDING_PIPELINE.processTokens(new CoNLLToken[]{this.tokens[left_shift],this.tokens[right_shift]});
//            for (float f: neighbor_vec) {ftrs.add(f);}
        }


        // embedding 特征
        float[] dependent_vec = SmoothNLP.WORDEMBEDDING_PIPELINE.processToken(this.tokens[dependentIndex]);
        float[] target_vec = SmoothNLP.WORDEMBEDDING_PIPELINE.processToken(this.tokens[targetIndex]);

        // add dependent target similarity
//        ftrs.add(cosineSimilarity(dependent_vec,target_vec));

        for (float f: dependent_vec) {ftrs.add(f);}
        for (float f: target_vec) {ftrs.add(f);}

        for (float f: this.tagProba[targetIndex]) {ftrs.add(f);}

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

//        float[][] tagProbas = SmoothNLP.POSTAG_PIPELINE.getAllProba(this.tokens);
//        ftr_size+= tagProbas[0].length;

        Float[][] all_ftrs = new Float[postiveIndexes.size()][ftr_size];
        int count = 0;
        for (int[] index : postiveIndexes){
            int i = index[0];
            int j = index[1];

            List<Float> ftrList = new LinkedList<>();
            for (float ftr: buildFtrs(i,j)){ ftrList.add(ftr); }
//            for (float ftr: tagProbas[j]){ ftrList.add(ftr); }

            all_ftrs[count] = ftrList.toArray(new Float[ftr_size]);
            count+=1;
        }
        return all_ftrs;

    }


    public Float[][] buildAllFtrs(){
        /**
         * build features for all token pair
         */
        boolean position = true;
        boolean neighbors = true;

        int ftr_size = buildFtrs(0,0,position,neighbors).length;
//        float[][] tagProbas = SmoothNLP.POSTAG_PIPELINE.getAllProba(this.tokens);
//        ftr_size+= tagProbas[0].length;

        if (selectedIndexes==null){
            Float[][] all_ftrs = new Float[(this.nodeSize)*(this.nodeSize)][ftr_size];
            for (int i = 0; i< this.nodeSize ;i++){
                for (int j =0; j< this.nodeSize; j++){
                    List<Float> ftrList = new LinkedList<>();
                    for (float ftr: buildFtrs(i,j,position,neighbors)){ ftrList.add(ftr); }
//                    for (float ftr: tagProbas[j]){ ftrList.add(ftr); }
                    all_ftrs[i*(this.nodeSize)+j] = ftrList.toArray(new Float[ftr_size]);
                }
            }
            return all_ftrs;
        }else{
            Float[][] all_ftrs = new Float[selectedIndexes.size()][ftr_size];
            int count = 0;
            for (int[] index : selectedIndexes){
                int i = index[0];
                int j = index[1];
                all_ftrs[count] = buildFtrs(i,j,position,neighbors);
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
        for (int i = 1; i < this.tokens.length; i++){
            labels[i-1] = this.tokens[i].relationship;
        }
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
        List<SToken> stokens  = new LinkedList<>();
        for (int i = 0; i< conllLines.length; i++){
            tokens[i+1] = CoNLLToken.parseCoNLLLine(conllLines[i]);
            stokens.add(new SToken(conllLines[i].split("\t")[1]));

        }
        stokens = SmoothNLP.POSTAG_PIPELINE.process(stokens);

        int counter = 1;
        for (SToken stoken: stokens){
            tokens[counter].postag = stoken.postag;
            counter+=1;
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

        long start = System.currentTimeMillis();

        System.out.println(UtilFns.toJson(g.buildAllTagFtrs()));
        long end = System.currentTimeMillis();
        System.out.print("tag feature time: ");
        System.out.println(end-start);

        System.out.println(g.buildAllFtrs()[0].length);
        System.out.println(g.buildAllTagFtrs()[0].length);
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
