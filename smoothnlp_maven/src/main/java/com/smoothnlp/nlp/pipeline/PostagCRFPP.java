package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.*;
import com.smoothnlp.nlp.model.crfagu.ModelImpl;
import com.smoothnlp.nlp.model.crfagu.Tagger;
import com.smoothnlp.nlp.model.crfagu.TaggerImpl;

//import com.smoothnlp.nlp.model.crfpp.ModelImpl;
//import com.smoothnlp.nlp.model.crfpp.Tagger;
//import com.smoothnlp.nlp.model.crfpp.TaggerImpl;

import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.smoothnlp.nlp.basic.IDictionary.MatchResult;



public class PostagCRFPP extends CRFModel{
    protected ModelImpl model;
    private Pattern pupattern,numpattern;
    private IDictionary tokenLibrary,datetimeLibrary;
    private static int sequenceTagNumber = 38;

    private static HashSet<String> validPostagSet = new HashSet<>();
    static {
        validPostagSet.add("AD");
        validPostagSet.add("AS");
        validPostagSet.add("CC");
        validPostagSet.add("CD");
        validPostagSet.add("CS");
        validPostagSet.add("DEC");
        validPostagSet.add("DEG");
        validPostagSet.add("DT");  // 限定词
//        validPostagSet.add("ETC");  // 等等
//        validPostagSet.add("IJ");  // count 742 开头无意义词
        validPostagSet.add("JJ");
        validPostagSet.add("LC");
        validPostagSet.add("M");
        validPostagSet.add("NN");
        validPostagSet.add("NR");
        validPostagSet.add("NT");
        validPostagSet.add("OD");  // 401 Ordinal Number 如: 第31
        validPostagSet.add("P");
        validPostagSet.add("PN");
//        validPostagSet.add("PU");
//        validPostagSet.add("SP");  // sentence final particle, 重要程度低
        validPostagSet.add("VA");
        validPostagSet.add("VC");
//        validPostagSet.add("VE");  // 有, 无, 字符较为但带哦
        validPostagSet.add("VV");
    }


    public PostagCRFPP(){
        this.model = new ModelImpl();
        this.model.open(SmoothNLP.CRF_POSTAG_MODEL,2,1,1.0);

        this.pupattern  = Pattern.compile("[\\s]+|[+——！“”|，。/？、~@#￥%……&*（）()》《丨\\[\\]]+");
        String numpattern = "[点两双一二三四五六七八九零十〇\\d.%十百千万亿]{2,8}";
        String old_numpattern = "[点两双一二三四五六七八九零十〇\\d|.|%|个|十|百|千|万|亿]+";
        this.numpattern = Pattern.compile(numpattern);

        this.datetimeLibrary = new SDictionary(new HashMap<String, String>() {
            {
                put("DTA", "datetime.txt");
                put("DTR", "datetime_relative.txt");
            }
        });

        this.tokenLibrary = new TrieDictionary(new HashMap<String, String>() {
            {
                put("CTY", "country.txt");
                put("LOC", "location.txt");
            }
        });
    }

    public String buildFtrs(String token, String[] ftrs){
        return token+"\t"+ join("\t",ftrs);
    }

    public void setActiveDictionaries(List<String> libraryNames){
        this.libraryNames = libraryNames;
    }

    public List<SToken> process(String input){
        List<SToken> stokens = SmoothNLP.SEGMENT_PIPELINE.process(input);
        return process(stokens);
    };

    public float[][] getAllProba(CoNLLToken[] stokens){

        Tagger tagger = this.model.createTagger();
        float[][] tokenTagProbas = new float[stokens.length][validPostagSet.size()];
        if (tagger==null){
            SmoothNLP.LOGGER.severe(String.format("CRF segment model is not properly read"));
            return null;
        }
        else {
            for (SToken stoken : stokens) {
                String ftr = super.buildFtrs(stoken.getToken());
                tagger.add(ftr);
            }
            tagger.parse();
            for (int i = 0; i < stokens.length; i++) {
                int counter = 0;
                for (int j = 0; j < sequenceTagNumber; j++) {
                   if (validPostagSet.contains(tagger.yname(j))){
                       tokenTagProbas[i][counter] = (float) tagger.prob(i, j);
                       counter+=1;
                   };
                }
            }
        }
        return tokenTagProbas;

    }

    public List<SToken> process(List<SToken> stokens){
        Tagger tagger = this.model.createTagger();
        if (tagger==null){
            SmoothNLP.LOGGER.severe(String.format("CRF segment model is not properly read"));
        }
        if (stokens == null || stokens.size() == 0) {
            return new ArrayList<SToken>();
        }else{
            for (SToken stoken: stokens){
                // 额外特征
//                String[] other_ftrs = new String[3];
//                other_ftrs[0] = Integer.toString(stoken.token.length());
//                String ftr = super.buildFtrs(stoken.getToken(),other_ftrs);
                String ftr = super.buildFtrs(stoken.getToken());
                tagger.add(ftr);
            }
            tagger.parse();
//            tagger.parseEmbedding();
            for (int  i=0; i<stokens.size();i++){

                int tagIndex = tagger.y(i);
//                if (tagIndex==0){  // 静止tagger 预测标点, 标点由强规则匹配
                double bestPorba = 0;

//                System.out.println(stokens.get(i).getToken());
                for (int j = 0; j<sequenceTagNumber; j++){
//                    System.out.println(tagger.yname(j) + ":"+tagger.prob(i,j));
                    if (tagger.prob(i,j)>bestPorba && !tagger.yname(j).equals("PU")){
                        tagIndex = j;
                        bestPorba = tagger.prob(i,j);
                    };
                }
//                }
//                System.out.println(tagIndex+":"+bestPorba);
                String ytag = tagger.yname(tagIndex);  // predict的t
                double tagproba = bestPorba;

                if (stokens.get(i).getToken().equals("、")){
                    // 对于中文中的顿号, 强制处理成CC-并列关系
                    ytag = "CC";
                    tagproba = 1.0;
                }

                // 对于字符串为"空格" 或者其他中文标点, 如"丨"的token, 强行改写postag = "PU"
                Matcher pumatcher = SmoothNLP.PUPattern.matcher(stokens.get(i).token);
                while (pumatcher.find()){
                    if (pumatcher.end() - pumatcher.start() == stokens.get(i).token.length()) {
                        ytag = "PU";
                        tagproba = 1.0;
                    }
                }

                Matcher numMatcher = SmoothNLP.NUMPattern.matcher(stokens.get(i).token);
                while (numMatcher.find()){
                    if (numMatcher.end() - numMatcher.start() == stokens.get(i).token.length()){
                        ytag = "CD";
                        tagproba = 1;
                    }
                }

                stokens.get(i).setPostag(ytag);
                stokens.get(i).setTagproba(tagproba);

                List<MatchResult> res = this.tokenLibrary.find(stokens.get(i).token);
                for (MatchResult m: res){
                    if (m.end-m.start == stokens.get(i).token.length() ) {  // 检验是否是完整匹配
                        stokens.get(i).setPostag(m.label);
                    }
                }

                res = this.datetimeLibrary.find(stokens.get(i).token);
                for (MatchResult m: res){
                    if (m.end-m.start == stokens.get(i).token.length() ) {
                        stokens.get(i).setPostag(m.label);
                    }
                }

            }
        }
        return stokens;
    };

    public static void main(String[] args){
        ISequenceTagger s = new PostagCRFPP();
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
    }

}
