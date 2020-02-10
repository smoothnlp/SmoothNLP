package com.smoothnlp.nlp.model.crfagu;

import com.smoothnlp.nlp.basic.UtilFns;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by zhifac on 2017/3/18.
 */
public abstract class
FeatureIndex {
    public static String[] BOS = {"_B-1", "_B-2", "_B-3", "_B-4", "_B-5", "_B-6", "_B-7", "_B-8"};
    public static String[] EOS = {"_B+1", "_B+2", "_B+3", "_B+4", "_B+5", "_B+6", "_B+7", "_B+8"};
    protected int maxid_;
    protected double[] alpha_;  // x 的参数
    protected float[] alphaFloat_;
    protected double costFactor_;
    protected int xsize_;  //训练文件中每个有效行通过"\t"进行split后的cols.length -1 ( 1 为label占位）
    protected boolean checkMaxXsize_;
    protected int max_xsize_;
    protected int threadNum_;
    protected List<String> unigramTempls_;  //存储unigram 模板
    protected List<String> bigramTempls_;   //存储bigram 模板
    protected String templs_;
    protected List<String> y_;  //label list (实际有效）
    protected List<List<Path>> pathList_;
    protected List<List<Node>> nodeList_;


    //支持embedding vector;

    protected int maxEmbeddingId_;
    protected boolean isSupportEmbedding = false;
    protected List<String> embeddingTempls_;  //存储embedding 特征模板 ，理论上仅支持 E00:%x[0,0], buildFeaturesEmbedding中仅取get(0)
    protected EmbeddingImpl embedding;  //存储embedding 特征本身
    protected double[] alphaEmbedding_; // embedding的参数


    public FeatureIndex() {
        maxid_ = 0;
        alpha_ = null;
        alphaFloat_ = null;
        costFactor_ = 1.0;
        xsize_ = 0;
        checkMaxXsize_ = false;
        max_xsize_ = 0;
        threadNum_ = 1;
        unigramTempls_ = new ArrayList<String>();
        bigramTempls_ = new ArrayList<String>();
        y_ = new ArrayList<String>();

        embeddingTempls_ = new ArrayList<String>();
        maxEmbeddingId_ = 0;
    }

    protected abstract int getID(String s);
    //protected abstract int getEmbeddingID(String s);

    /**
     * 节点代价计算函数
     * @param node
     */
    public void calcCost(Node node) {
        node.cost = 0.0;
        if (alphaFloat_ != null) {
            float c = 0.0f;
            for (int i = 0; node.fVector.get(i) != -1; i++) {
                c += alphaFloat_[node.fVector.get(i) + node.y];
            }
            node.cost = costFactor_ * c;
        } else { //crf++ 初始化的是 alpha_变量，调用该部分
            double c = 0.0;
            for (int i = 0; node.fVector.get(i) != -1; i++) {
                c += alpha_[node.fVector.get(i) + node.y];
            }
            node.cost = costFactor_ * c;
        }
    }
    /**
     * 边的代价计算函数
     * @param path
     */
    public void calcCost(Path path) {
        path.cost = 0.0;
        if (alphaFloat_ != null) {
            float c = 0.0f;
            for (int i = 0; path.fvector.get(i) != -1; i++) {
                c += alphaFloat_[path.fvector.get(i) + path.lnode.y * y_.size() + path.rnode.y];
            }
            path.cost = costFactor_ * c;
        } else {
            double c = 0.0;
            for (int i = 0; path.fvector.get(i) != -1; i++) {
                c += alpha_[path.fvector.get(i) + path.lnode.y * y_.size() + path.rnode.y];
            }
            path.cost = costFactor_ * c;
        }
    }

    /**
     * 引入Embedding之后的Node的 代价计算函数
     * @param node
     */
    public void calcCostWithEmbedding(Node node){

        node.cost = 0.0;
        double c = 0.0;
        for (int i = 0; node.fVector.get(i) != -1; i++) {
            c += alpha_[node.fVector.get(i) + node.y];
        }

        //float [] vector = embedding.getStrEmbedding(node.emStr);
        float[] vector = embedding.getArrayStrEmbedding(node.emStrs);
        int vectorSize = vector.length;

        if(vector.length>0){
            for (int i=0; i< vector.length;i++){
                //c+= alphaEmbedding_[i + node.y * getEmbeddingVectorSize()] * vector[i];
                c+= alphaEmbedding_[i + node.y * vectorSize] * vector[i];
                //System.out.println(vector.length+ " :: " + vectorSize);
            }

        }

        node.cost = costFactor_ * c;
    }

    /**
     * 引入Embedding之后的Node的 代价计算函数, 对于非bigram 特征时无用处
     * @param path
     */
    public void calcCostWithEmbedding(Path path){
        path.cost = 0.0;
        double c = 0.0;
        for (int i = 0; path.fvector.get(i) != -1; i++) {
            c += alpha_[path.fvector.get(i) + path.lnode.y * y_.size() + path.rnode.y];
        }
        path.cost = costFactor_ * c;

    }



    public String makeTempls(List<String> unigramTempls, List<String> bigramTempls) {
        StringBuilder sb = new StringBuilder();
        for (String temp: unigramTempls) {
            sb.append(temp).append("\n");
        }
        for (String temp: bigramTempls) {
            sb.append(temp).append("\n");
        }

        return sb.toString();
    }

    public String getTemplate() {
        return templs_;
    }

    /**
     * 作用获取 TaggerImpl 的 x_ 中当前行 对应于 idxStr所示模板指定的 字符 or token
     * @param idxStr：模板指定的取数位置[-2,0] (String[])
     * @param cur:当前位于训练数据的第cur+1行
     * @param tagger
     * @return
     *   如果是cur=0, U00:[-1,0], 对于第一行，并无前一个词，则返回"_B-1"
     *   如果是cur=0, U01:[0,0], 对于第一行，返回其字符值
     */
    public String getIndex(String[] idxStr, int cur, TaggerImpl tagger) {
        int row = Integer.valueOf(idxStr[0]); // 模板设置为偏离于当前行的行数即 -2
        int col = Integer.valueOf(idxStr[1]); // 取模型文件中的第几列
        int pos = row + cur;  // 实际代取的字符的在x中的位置
        if (row < -EOS.length || row > EOS.length || col < 0 || col >= tagger.xsize()) {
            return null;
        }

        //TODO(taku): very dirty workaround
        if (checkMaxXsize_) {
            max_xsize_ = Math.max(max_xsize_, col + 1);
        }
        if (pos < 0) {
            return BOS[-pos - 1];
        } else if (pos >= tagger.size()) {
            return EOS[pos - tagger.size()];
        } else {
            return tagger.x(pos, col);
        }
    }

    /**
     * 根据模板,以%x进行split, 生成sb = U00:每
     * case：
     *  每
     *  日
     *  新 <== 扫描至该行，U00:%x[0,0] 生成 "U00:新"
     * 生成: U00:每
     * @param str，单个特征模板 如：U00:%x[-2,0]
     * @param cur
     * @param tagger
     * @return
     */
    public String applyRule(String str, int cur, TaggerImpl tagger) {
        StringBuilder sb = new StringBuilder();
        for (String tmp : str.split("%x", -1)) {
            if (tmp.startsWith("U") || tmp.startsWith("B")) {
                sb.append(tmp);
            } else if (!tmp.startsWith("E") && tmp.length() > 0) {
                String[] tuple = tmp.split("]");
                String[] idx = tuple[0].replace("[", "").split(",");
                String r = getIndex(idx, cur, tagger);
                if (r != null) {
                    sb.append(r);
                }
                if (tuple.length > 1) {
                    sb.append(tuple[1]);
                }
            }
        }

        return sb.toString();
    }

    /**
     *
     * @param feature 当前位置对应特征模板所 生成的特征ID值
     * @param templs 特征模板
     * @param curPos 当前位置，位于训练文件第curPos行
     * @param tagger
     * @return
     */

    private boolean buildFeatureFromTempl(List<Integer> feature, List<String> templs, int curPos, TaggerImpl tagger) {
        for (String tmpl : templs) {
            String featureID = applyRule(tmpl, curPos, tagger); //  当前词(curPos) 以及当前的特征（tmpl) 生成一个特征放在featureID
            if (featureID == null || featureID.length() == 0) {
                System.err.println("format error");
                return false;
            }
            int id = getID(featureID); // 根据特征的featureID，或者该特征的id;如不存在该特征则生成新的ID，将该ID添加至feature变量中；
            if (id != -1) {
                feature.add(id);  //将该ID键入feature中
            }
        }
        return true;
    }

    // 根据模板 unigramTempls \bigramTempls\  embeddingTempls 构建 features
    private boolean buildFeatureFromEmbeddingTempl(ArrayList<String> embeddingStrs, List<String> templs, int curPos, TaggerImpl tagger){
        for(String template:templs){
            String embeddingStr = applyRule(template, curPos, tagger);
            embeddingStrs.add(embeddingStr);
        }
        return true;
    }
    /**
     * featureCache 未起实质性作用，EncoderFeatureIndex中更新了maxid 和 dic_
     * featureCache 中存储的是对应与单句中每行对应于生成的特征(id list)
     *
     * @param tagger
     * @return
     */
    public boolean buildFeatures(TaggerImpl tagger) {
        List<Integer> feature = new ArrayList<Integer>();
        List<List<Integer>> featureCache = tagger.getFeatureCache_();
        tagger.setFeature_id_(featureCache.size());  // 标记？取该句的特征从该ID位置进行获取，C++中的操作，Java 待后续

        for (int cur = 0; cur < tagger.size(); cur++) {   //遍历每个词，计算每个词的特征；
            if (!buildFeatureFromTempl(feature, unigramTempls_, cur, tagger)) {  // 根据unigram ,遍历 每个unigram 的特征；
                return false;
            }
            feature.add(-1);  // 该词对应unigramTempls生成特征完毕；添加标志位-1 ？
            featureCache.add(feature);  // 将对应的当前tagger对应的featureIDlist 加入featureCache中；
            feature = new ArrayList<Integer>();
        }
        for (int cur = 1; cur < tagger.size(); cur++) {
            if (!buildFeatureFromTempl(feature, bigramTempls_, cur, tagger)) {
                return false;
            }
            feature.add(-1);
            featureCache.add(feature);
            feature = new ArrayList<Integer>();
        }
        return true;
    }


    /**
     * 根据 tagger.featureCache_进行 调用 TaggerImpl.set_node() 进行Node初始化，
     * @param tagger
     */
    public void rebuildFeatures(TaggerImpl tagger) {
        int fid = tagger.getFeature_id_();
        List<List<Integer>> featureCache = tagger.getFeatureCache_();
        for (int cur = 0; cur < tagger.size(); cur++) {
            List<Integer> f = featureCache.get(fid++);  // 去除词的特征，词的特征列表对应特征模板里的Unigram特征
            for (int i = 0; i < y_.size(); i++) {  // label list
                Node n = new Node();
                n.clear();
                n.x = cur;  // 一个句子中的第几行，即第几个词
                n.y = i;   // 设置为第几个label
                n.fVector = f;    // 特征列表
                tagger.set_node(n, cur, i);   // TaggerImpl 中的二位数组node_存放该节点
            }
        }

        //从第二个词开始构造节点之间的边，两个词之间有y_.size()*y_.size()条边
        for (int cur = 1; cur < tagger.size(); cur++) {
            List<Integer> f = featureCache.get(fid++);  //取出边的特征序列，边的特征列表对应特征模板里的Bigram特征
            for (int j = 0; j < y_.size(); j++) {
                for (int i = 0; i < y_.size(); i++) {
                    Path p = new Path();
                    p.clear();
                    p.add(tagger.node(cur - 1, j), tagger.node(cur, i)); //add函数设置边的左右节点，并将当前边加入左右节点的边集合中；
                    p.fvector = f;
                }
            }
        }
    }


    /**
     * embeddingTempls,仅取第一个构建,且默认为E00:[0,0]
     *
     * 构建包括 Unigram, Bigram, 和 Embedding 特征
     *
     * @param tagger
     * @return
     */
    public boolean buildEmbeddingFeature(TaggerImpl tagger){

        List<Integer> feature = new ArrayList<Integer>();
        List<List<Integer>> featureCache = tagger.getFeatureCache_();
        tagger.setFeature_id_(featureCache.size());  // 标记？取该句的特征从该ID位置进行获取，C++中的操作，Java 待后续

        for (int cur = 0; cur < tagger.size(); cur++) {   //遍历每个词，计算每个词的特征；
            if (!buildFeatureFromTempl(feature, unigramTempls_, cur, tagger)) {  // 根据unigram ,遍历 每个unigram 的特征；
                return false;
            }
            feature.add(-1);  // 该词对应unigramTempls生成特征完毕；添加标志位-1 ？
            featureCache.add(feature);  // 将对应的当前tagger对应的featureIDlist 加入featureCache中；
            feature = new ArrayList<Integer>();
        }
        for (int cur = 1; cur < tagger.size(); cur++) {
            if (!buildFeatureFromTempl(feature, bigramTempls_, cur, tagger)) {
                return false;
            }
            feature.add(-1);
            featureCache.add(feature);
            feature = new ArrayList<Integer>();
        }
        // embedding feature ;
        ArrayList<String> embeddingStrs = new ArrayList<>();
        List<ArrayList<String>> embeddingStrsCache = tagger.getFeatureEmbeddingStrsCache_();

        if (embeddingTempls_.size()>=1){  // 如果模板中存在Embedding特征, 处理Embedding特征, <- 对不添加Embedding特征的模型进行兼容
            //String template = embeddingTempls_.get(0); // 仅支持一个embedding特征；
            //for(int cur = 0; cur<tagger.size(); cur++){
            //    String embeddingStr = applyRule(template, cur, tagger); // 暂时还是 E00:每；
            //    embeddingStrs.add(embeddingStr);
            //}
            for(int cur = 0 ; cur < tagger.size(); cur++){
                if(!buildFeatureFromEmbeddingTempl(embeddingStrs,embeddingTempls_,cur,tagger)){
                    System.err.println("error in build embedding templates");
                    return false;
                }
                embeddingStrsCache.add(embeddingStrs);
                embeddingStrs = new ArrayList<>();
            }
            //StringBuffer sb = new StringBuffer();
            //for(ArrayList<String> str:embeddingStrsCache){
            //    sb.append(str+"\t");
            //}
            //System.out.println(sb.toString());

        }else{
            System.err.println("目前crfagu仅仅支持拥有Embedding特征的选项, 暂不支持没有Embedding模型的情况");
        }
        return true;
    }

    public void rebuildEmbeddingFeatures(TaggerImpl tagger){
        int fid = tagger.getFeature_id_();
        List<List<Integer>> featureCache = tagger.getFeatureCache_();
        // embedding 所要支持的key;
        List<ArrayList<String>> featureEmbeddingStrsCache= tagger.getFeatureEmbeddingStrsCache_();

        for (int cur = 0; cur < tagger.size(); cur++) {
            List<Integer> f = featureCache.get(fid++);  // 去除词的特征，词的特征列表对应特征模板里的Unigram特征

            ArrayList<String> emStrs;
            if (featureEmbeddingStrsCache.size()==0){
                emStrs = new ArrayList<>();
//                continue;  // 处理不使用 Embedding Feature 的情况, 兼容普通版本crfpp
            }else{
                emStrs = featureEmbeddingStrsCache.get(cur);
            }



            for (int i = 0; i < y_.size(); i++) {  // label list
                Node n = new Node();
                n.clear();
                n.x = cur;  // 一个句子中的第几行，即第几个词
                n.y = i;   // 设置为第几个label
                n.fVector = f;    // 特征列表
                n.emStrs = emStrs;
                tagger.set_node(n, cur, i);   // TaggerImpl 中的二位数组node_存放该节点
               // System.out.println(n.emStrs);
            }
        }

        //从第二个词开始构造节点之间的边，两个词之间有y_.size()*y_.size()条边
        for (int cur = 1; cur < tagger.size(); cur++) {
            List<Integer> f = featureCache.get(fid++);  //取出边的特征序列，边的特征列表对应特征模板里的Bigram特征
            for (int j = 0; j < y_.size(); j++) {
                for (int i = 0; i < y_.size(); i++) {
                    Path p = new Path();
                    p.clear();
                    p.add(tagger.node(cur - 1, j), tagger.node(cur, i)); //add函数设置边的左右节点，并将当前边加入左右节点的边集合中；
                    p.fvector = f;
                }
            }
        }


    }


    public boolean open(String file) {
        return true;
    }

    public boolean open(InputStream stream) {
        return true;
    }

    public void clear() {

    }

    public int size() {
        return getMaxid_();
    }

    public int ysize() {
        return y_.size();
    }

    public int getMaxid_() {
        return maxid_;  // 特征数 ，如果只有unigram ,  = |unique words| * (Y_) ； 如果是bigram = |unique words| * (Y_) * (Y_)
    }

    public void setMaxid_(int maxid_) {
        this.maxid_ = maxid_;
    }

    public double[] getAlpha_() {
        return alpha_;
    }

    public void setAlpha_(double[] alpha_) {
        this.alpha_ = alpha_;
    }

    public float[] getAlphaFloat_() {
        return alphaFloat_;
    }

    public void setAlphaFloat_(float[] alphaFloat_) {
        this.alphaFloat_ = alphaFloat_;
    }

    public double getCostFactor_() {
        return costFactor_;
    }

    public void setCostFactor_(double costFactor_) {
        this.costFactor_ = costFactor_;
    }

    public int getXsize_() {
        return xsize_;
    }

    public void setXsize_(int xsize_) {
        this.xsize_ = xsize_;
    }

    public int getMax_xsize_() {
        return max_xsize_;
    }

    public void setMax_xsize_(int max_xsize_) {
        this.max_xsize_ = max_xsize_;
    }

    public int getThreadNum_() {
        return threadNum_;
    }

    public void setThreadNum_(int threadNum_) {
        this.threadNum_ = threadNum_;
    }

    public List<String> getUnigramTempls_() {
        return unigramTempls_;
    }

    public void setUnigramTempls_(List<String> unigramTempls_) {
        this.unigramTempls_ = unigramTempls_;
    }

    public List<String> getBigramTempls_() {
        return bigramTempls_;
    }

    public void setBigramTempls_(List<String> bigramTempls_) {
        this.bigramTempls_ = bigramTempls_;
    }

    public List<String> getY_() {
        return y_;
    }

    public void setY_(List<String> y_) {
        this.y_ = y_;
    }

    public List<List<Path>> getPathList_() {
        return pathList_;
    }

    public void setPathList_(List<List<Path>> pathList_) {
        this.pathList_ = pathList_;
    }

    public List<List<Node>> getNodeList_() {
        return nodeList_;
    }

    public void setNodeList_(List<List<Node>> nodeList_) {
        this.nodeList_ = nodeList_;
    }

    // ---------------// ---------------// ---------------
    // ----------- support embedding vector --------------
    // ---------------// ---------------// ---------------
    public int getMaxEmbeddingId_(){
        return maxEmbeddingId_;
    }
    public int getEmbeddingVectorSize(){
        return embedding.getVsize();
    }
    public int sizeEmbedding(){
        return getEmbeddingVectorSize() * ysize() * embeddingTempls_.size();
    }
    public void setAlphaEmbedding_(double[] alphaEmbedding_){
        this.alphaEmbedding_ = alphaEmbedding_;
    }

}
