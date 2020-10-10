package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.CoNLLToken;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.basic.UtilFns;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;

public class PostagXgboost extends BaseSequenceTagger{

    private Booster potagModel;

    private float[][] tokenTagProbas;
    private boolean loaded;

    private static Map<Integer,String> index2tag;
    static {
        index2tag = new HashMap<>();
        index2tag.put(0,"AD");
        index2tag.put(1,"AS");
        index2tag.put(2,"BA");
        index2tag.put(3,"CC");
        index2tag.put(4,"CD");
        index2tag.put(5,"CS");
        index2tag.put(6,"DEC");
        index2tag.put(7,"DEG");
        index2tag.put(8,"DER");
        index2tag.put(9,"DEV");
        index2tag.put(10,"DT");
        index2tag.put(11,"ETC");
        index2tag.put(12,"FRAG");
        index2tag.put(13,"FW");
        index2tag.put(14,"IJ");
        index2tag.put(15,"JJ");
        index2tag.put(16,"LB");
        index2tag.put(17,"LC");
        index2tag.put(18,"M");
        index2tag.put(19,"MSP");
        index2tag.put(20,"NN");
        index2tag.put(21,"NOI");
        index2tag.put(22,"NR");
        index2tag.put(23,"NT");
        index2tag.put(24,"OD");
        index2tag.put(25,"ON");
        index2tag.put(26,"P");
        index2tag.put(27,"PN");
        index2tag.put(28,"PU");
        index2tag.put(29,"SB");
        index2tag.put(30,"SP");
        index2tag.put(31,"URL");
        index2tag.put(32,"VA");
        index2tag.put(33,"VC");
        index2tag.put(34,"VE");
        index2tag.put(35,"VV");
    }

    private static Map<String, Float> tag2float;
    static {
        tag2float = new HashMap<>();
        tag2float.put("AD",0.0f);
        tag2float.put("AS",1.0f);
        tag2float.put("BA",2.0f);
        tag2float.put("CC",3.0f);
        tag2float.put("CD",4.0f);
        tag2float.put("CS",5.0f);
        tag2float.put("DEC",6.0f);
        tag2float.put("DEG",7.0f);
        tag2float.put("DER",8.0f);
        tag2float.put("DEV",9.0f);
        tag2float.put("DT",10.0f);
        tag2float.put("ETC",11.0f);
        tag2float.put("FRAG",12.0f);
        tag2float.put("FW",13.0f);
        tag2float.put("IJ",14.0f);
        tag2float.put("JJ",15.0f);
        tag2float.put("LB",16.0f);
        tag2float.put("LC",17.0f);
        tag2float.put("M",18.0f);
        tag2float.put("MSP",19.0f);
        tag2float.put("NN",20.0f);
        tag2float.put("NOI",21.0f);
        tag2float.put("NR",22.0f);
        tag2float.put("NT",23.0f);
        tag2float.put("OD",24.0f);
        tag2float.put("ON",25.0f);
        tag2float.put("P",26.0f);
        tag2float.put("PN",27.0f);
        tag2float.put("PU",28.0f);
        tag2float.put("SB",29.0f);
        tag2float.put("SP",30.0f);
        tag2float.put("URL",31.0f);
        tag2float.put("VA",32.0f);
        tag2float.put("VC",33.0f);
        tag2float.put("VE",34.0f);
        tag2float.put("VV",35.0f);
    }

    public PostagXgboost(){
        this.loaded = false;
    }

    public void init(){
        potagModel = UtilFns.loadXgbModel(SmoothNLP.XGBoost_Postag_Model);
    }

    public float[][] getAllProba(CoNLLToken[] stokens){
        return this.tokenTagProbas;
    }

    public List<SToken> process(String input){
        List<SToken> tokens = SmoothNLP.SEGMENT_PIPELINE.process(input);
        return process(tokens);
    };

    public List<SToken> process(List<SToken> tokens){

        if (!this.loaded){
            this.init();
            this.loaded = true;
        }

        int token_size = tokens.size();
        if (token_size<=0){
            return tokens;
        }
        Float[] ftrs = parseSentenceFtr(tokens);
        float[] all_ftr_array = new float[ftrs.length];
        int counter = 0;
        for (float f: ftrs){
            all_ftr_array[counter] = f;
            counter+=1;
        }
        int ftr_size = all_ftr_array.length/token_size;
        try{
            DMatrix dmatrix = new DMatrix(all_ftr_array,token_size,ftr_size,Float.NaN);
            float[][] predictprobas =  this.potagModel.predict(dmatrix,false);

            tokenTagProbas = new float[tokens.size()+1][PostagCRFPP.validPostagSet.size()];

            for (int i=0; i<token_size;i++){
                SToken token = tokens.get(i);
                float[] probas = predictprobas[i];
                int max_index = 0;

                int ycounter = 0;
                for (int index =0; index<probas.length; index+=1 ){
                    if (probas[index] > probas[max_index]){
                        max_index = index;
                    }

                    // 存储token proba
                    if (PostagCRFPP.validPostagSet.contains(index2tag.get(index))){
                        tokenTagProbas[i+1][ycounter] = probas[index];
                        ycounter+=1;
                    };

                }
                token.setPostag(index2tag.get(max_index));
                token.setTagproba(probas[max_index]);
            }

        }catch (XGBoostError e){
            System.err.println(e);
        }

        for (SToken token : tokens){
            try{
                Float.parseFloat(token.token);
                token.setPostag("CD");
                token.setTagproba(0.99);
            }catch (NumberFormatException e){

            }
        }

        return tokens;
    }

    public List<SToken> postProcess(List<SToken> stokens){
        for (int  i=0; i<stokens.size();i++){
            String ytag = stokens.get(i).getPostag();
            float tagproba = stokens.get(i).getTagproba();

            if (stokens.get(i).getToken().equals("、")){
                // 对于中文中的顿号, 强制处理成CC-并列关系
                ytag = "CC";
                tagproba = 1.0f;
            }

            Matcher numMatcher = SmoothNLP.NUMPattern.matcher(stokens.get(i).token);
            while (numMatcher.find()){
                if (numMatcher.end() - numMatcher.start() == stokens.get(i).token.length()){
                    ytag = "CD";
                    tagproba = 1.0f;
                }
            }

            stokens.get(i).setPostag(ytag);
            stokens.get(i).setTagproba(tagproba);

        }
        return stokens;

    }


    public static Float[] parseSentenceTag(List<SToken> sentence){
        LinkedList<Float> labels = new LinkedList<>();
        for (SToken token : sentence){
            if (!tag2float.containsKey(token.getPostag())){
                System.err.println(UtilFns.toJson(sentence));
            }
            labels.add(tag2float.get(token.getPostag()));
        }
        return labels.toArray(new Float[labels.size()]);
    }

    public static Float[] parseSentenceFtr(List<SToken> sentence){
        int ngram_shift = 3;
        List<List<SToken>> shifted_sentence = new LinkedList<>();

        LinkedList<Float> ftrs = new LinkedList<>();
        int index = 0;
        for (SToken token : sentence){
            float [] embedFtr = SmoothNLP.WORDEMBEDDING_PIPELINE.processToken(token);
            for (float f: embedFtr){ftrs.add(f);}

            for (int i =1; i<=ngram_shift;i++){
                if (index-i<0){
                    String dummy = "S"+(index-i);
                    for (float f: SmoothNLP.WORDEMBEDDING_PIPELINE.processToken(dummy)){ftrs.add(f);}
                }else{
                    for (float f: SmoothNLP.WORDEMBEDDING_PIPELINE.processToken(sentence.get(index-i))){ftrs.add(f);}
                }
                if (index+i>=sentence.size()){
                    String dummy = "S"+(index+i-sentence.size());
                    for (float f: SmoothNLP.WORDEMBEDDING_PIPELINE.processToken(dummy)){ftrs.add(f);}
                }else{
                    for (float f: SmoothNLP.WORDEMBEDDING_PIPELINE.processToken(sentence.get(index+i))){ftrs.add(f);}
                }
            }
            index+=1;
        }
        return ftrs.toArray(new Float[ftrs.size()]);
    }

    public static DMatrix parseDataset2Dmatrix(List<List<SToken>> dataset) throws XGBoostError {
        int ftr_size = -1;
        int record_counter = 0;
        LinkedList<Float> all_ftrs = new LinkedList<>();
        LinkedList<Float> all_labels = new LinkedList<>();
        for (List<SToken> sentence : dataset){

            Float[] sentence_ftr = parseSentenceFtr(sentence);
            Float[] sentence_label = parseSentenceTag(sentence);
            boolean invalid_label = false;
            for (Float f : sentence_label){
                if (f==null){
                    invalid_label = true;
                    System.err.println(UtilFns.toJson(sentence));
                }
            }
            if (invalid_label){continue;}
            record_counter+=sentence.size();
            ftr_size = sentence_ftr.length/sentence_label.length;
            for (float f: sentence_ftr){ all_ftrs.add(f);}
            for (float f: sentence_label){ all_labels.add(f);}
        }
        float[] all_ftr_array = new float[all_ftrs.size()];
        int counter = 0;
        for (float f: all_ftrs){
            all_ftr_array[counter] = f;
            counter+=1;
        }
        float[] all_label_array = new float[all_labels.size()];
        counter = 0;
        for (float f: all_labels){
            all_label_array[counter] = f;
            counter+=1;
        }
        DMatrix dmatrix = new DMatrix(all_ftr_array,record_counter,ftr_size,Float.NaN);
        dmatrix.setLabel(all_label_array);
        System.out.println(" Feature size "+ftr_size);
        System.out.println(" Record Size "+record_counter);
        System.out.println(" ALL Feature size "+all_ftr_array.length);
        System.out.println(" ALL Label size "+all_label_array.length);
        return dmatrix;
    }

    public static List<List<SToken>> readCoNLLLines2sentences(String CoNLLFile) throws IOException {
        InputStream in = SmoothNLP.IOAdaptor.open(CoNLLFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = null;
        List<String[]> conll_docs = new ArrayList<String[]>();
        ArrayList<String> conll_document = new ArrayList<String>();
        int line_counter = 0;
        while ((line = reader.readLine()) != null){
            if (line.equals("")){
                conll_docs.add(conll_document.toArray(new String[conll_document.size()]));
                conll_document = new ArrayList<String>();
            }else{
                conll_document.add(line);
                line_counter+=1;
            }
            if (line_counter%100==0){
            }
        }

        conll_docs.add(conll_document.toArray(new String[conll_document.size()]));

        System.out.println(String.format("~~~ Done with reading all CoNLL documents %d ~~~", conll_docs.size()));

        List<List<SToken>> dataset = new LinkedList<>();
        for (String[] cdoc: conll_docs){
            List<SToken> sentence = new LinkedList<>();
            for (String cdoc_token: cdoc){
                String[] splits =  cdoc_token.split("\t");
                if (splits.length != 2){
                    System.err.println("格式错误");
                    System.err.println(UtilFns.toJson(cdoc));
                    System.err.println(UtilFns.toJson(cdoc_token));
                    break;
                }
                SToken token = new SToken(splits[0],splits[1]);
                sentence.add(token);
            }
            if (sentence.size()>=1){  // 空句不添加
                dataset.add(sentence);
            }

//            System.out.println(UtilFns.toJson(sentence));
        }
        return dataset;
    }

    public static void train(String trainFile, String devFile, String modelAddr, int nround, int earlyStops, int nthreads) throws IOException,XGBoostError{
        List<List<SToken>> trainDataset = readCoNLLLines2sentences(trainFile);
        List<List<SToken>> devDataset = readCoNLLLines2sentences(devFile);
        final DMatrix trainMatrix = parseDataset2Dmatrix(trainDataset);
        final DMatrix devMatrix = parseDataset2Dmatrix(devDataset);

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("nthread", nthreads);
                put("max_depth", 16);
                put("silent", 0);
                put("objective", "multi:softprob");
                put("colsample_bytree",0.90);
                put("colsample_bylevel",0.90);
                put("eta",0.2);
                put("subsample",0.95);
                put("lambda",0.8);

                // tree methods for regulation
                put("min_child_weight",5);
                put("max_leaves",256);

                put("eval_metric", "merror");
                put("tree_method","approx");
                put("num_class",tag2float.size());
            }
        };
        Map<String, DMatrix> watches = new HashMap<String, DMatrix>() {
            {
                put("train", trainMatrix);
                put("dev",devMatrix);
            }
        };

        Booster booster = XGBoost.train(trainMatrix, params, nround, watches, null, null,null,earlyStops);
        OutputStream outstream = SmoothNLP.IOAdaptor.create(modelAddr);
        booster.saveModel(outstream);

    }

    public static void main(String[] args) throws Exception{
//        String[] doc = new String[]{
//                "完美\tJJ\n",
//                "日记\tNN\n"
//        };
//        List<List<SToken>> dataset = PostagXgboost.readCoNLLLines2sentences("gold_finance.txt");
//        List<List<SToken>> dataset = PostagXgboost.readCoNLLLines2sentences("train_dev_gold.conllx");
//        PostagXgboost.parseDataset2Dmatrix(dataset);
        if (args.length>=1){
            PostagXgboost.train(args[0],args[1],args[2], Integer.parseInt(args[3]),Integer.parseInt(args[4]),Integer.parseInt(args[5]));
        }else{
            PostagXgboost s = new PostagXgboost();
            System.out.println(s.process("五十块钱买了两个冰淇淋还是挺便宜的"));
            System.out.println((s.process("广汽集团上月利润达到5万,相比同期增长5%")));
            System.out.println((s.process("广汽集团上月利润达到5万 相比同期增长5%")));
            System.out.println(s.process("一度被政策冷落的油电混合动力汽车，有可能被重新加注鼓励的法码。"));
            System.out.println((s.process("“芬木科技”完成千万级人民币天使轮融资，投资方为思得投资、薛蛮子")));
            System.out.println((s.process("据了解，三七互娱旗下首款云游戏已在开发当中，未来将登陆华为云游戏平台。")));
            System.out.println((s.process("百度创新性地推出了基于搜索的营销推广服务")));
            System.out.println((s.process("腾讯")));
            System.out.println((s.process("阿里与腾讯达成合作协议")));
            System.out.println((s.process("阿里巴巴与腾讯达成合作协议")));
            System.out.println((s.process("华为与腾讯达成合作协议")));
            System.out.println((s.process("预估值180亿 首钢股份获注迁安钢铁")));
            System.out.println((s.process("“试玩互动”获超过1千万元天使轮融资，游戏广告可“先试玩再下载”")));
            System.out.println((s.process("这之后，百度还发布了好运中国年、AI办事处等十多个即将在2020年推出的超级IP，对用户的精细化运营正在为百度创造更大的商业空间。")));
            System.out.println((s.process("首发|借助AI智能升级传统餐饮业 艾唯尔科技获东方富海数千万元Pre-A轮融资")));
            System.out.println((s.process("百度是全球最大的中文搜索引擎，2000年1月由李彦宏、徐勇两人创立于北京中关村，致力于向人们提供“简单，可依赖”的信息获取方式")));
            System.out.println(s.process("SmoothNLP在V0.3版本中正式推出知识抽取功能"));
            System.out.println((s.process("上岛咖啡于1968年进驻于宝岛台湾开始发展")));
            System.out.println((s.process("据了解，三七互娱旗下首款云游戏已在开发当中，未来将登陆华为云游戏平台。"))); // 华为 vv
            System.out.println((s.process("云计算将推动物联网产业发展")));
            System.out.println((s.process("字节跳动与腾讯达成合作协议")));
            System.out.println((s.process("嗅问将是上海文磨网络科技开发的一款信息整合产品")));
            System.out.println((s.process("呼吸道飞沫传染是新型冠状病毒的主要传播途径")));
            System.out.println((s.process("新款讴歌TL登上环保目录")));
            System.out.println(s.process("AI写作产品的核心是定位用户需求"));
            System.out.println(s.process("腾讯进军印度保险市场：花15亿元收购一公司10%股份 ")); // 腾讯 AD
            System.out.println(s.process("国产特斯拉Model3宣布降价至29.9万元")); // 特斯拉 vv
            System.out.println((s.process("借助AI智能升级传统餐饮业,艾唯尔科技获东方富海数千万元Pre-A轮融资")));
            System.out.println(s.process("四川内江查获50余万个三无口罩"));
            System.out.println(s.process("POSIX标准是由IEEE和ISO/IEC共同开发的标准系统"));
            System.out.println(s.process("中国银行是香港、澳门地区的发钞行，业务范围涵盖商业银行、投资银行、基金、保险、航空租赁等"));
            System.out.println(s.process("口罩是一种卫生用品，一般指戴在口鼻部位用于过滤进入口鼻的空气，以达到阻挡有害的气体、气味、飞沫进出佩戴者口鼻的用具，以纱布或纸等制成。"));
            System.out.println(s.process("环境照明产品：LED洗墙灯、LED护栏管、LED灯带"));
            System.out.println(s.process("阿里巴巴CEO张勇"));
            System.out.println(s.process("新款讴歌TL登上环保目录"));
            System.out.println(s.process("腾讯进军印度保险市场：花15亿元收购一公司10%股份"));
            System.out.println(s.process("七喜电脑股份有限公司其前身为1997年8月成立的七喜电脑有限公司"));
            System.out.println(s.process("百度一下你就知道"));
            System.out.println(s.process("百度创新性地推出了基于搜索的营销推广服务"));
            System.out.println(s.process("完美日记"));
            System.out.println(s.process("为什么特斯拉给汽车产业链上市公司带来的，不仅仅是订单？"));
            System.out.println(s.process("小鹏汽车获得新一轮融资"));
            System.out.println(s.process("萨德系统零部件供应商"));
            System.out.println(s.process("拼多多第三季度营收达到3亿美金"));
            System.out.println(s.process("腾讯云IoT与意法半导体宣布在物联网方面展开合作"));
            System.out.println(s.process("时彩公司和农业基地签约仪式"));
            System.out.println(s.process("发脾气对孩子有多大影响"));

            System.out.println(s.process("“红旗漫卷西风”一词"));
            System.out.println(s.process("“意大利志愿军的出色表现"));
            System.out.println(s.process("“德国总理默克尔、法国总统奥朗德 "));
            System.out.println(s.process("紫砂茶具"));

            System.out.println(s.process("截至3月，我国网民规模为9.04亿，较2018年底增长7508万；"));
            System.out.println(s.process("腾讯股票暴跌10%"));
            System.out.println(s.process("腾讯进军印度保险市场：花15亿元收购一公司10%股份"));
            System.out.println(s.process("七喜电脑股份有限公司其前身为1997年8月成立的七喜电脑有限公司"));


        }
    }

}
