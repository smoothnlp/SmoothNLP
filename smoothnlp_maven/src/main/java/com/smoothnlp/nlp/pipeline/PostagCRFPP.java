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


    public PostagCRFPP(){
        this.model = new ModelImpl();
        this.model.open(SmoothNLP.CRF_POSTAG_MODEL,0,1,1.0);

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

                String ytag = tagger.yname(tagger.y(i));  // predict的t
                double tagproba = tagger.prob(i);

                // 对于字符串为"空格" 或者其他中文标点, 如"丨"的token, 强行改写postag = "PU"
                Matcher pumatcher = this.pupattern.matcher(stokens.get(i).token);
                while (pumatcher.find()){
                    if (pumatcher.end() - pumatcher.start() == stokens.get(i).token.length()) {
                        ytag = "PU";
                        tagproba = 1.0;
                    }
                }

                Matcher numMatcher = this.numpattern.matcher(stokens.get(i).token);
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
        System.out.println((s.process("阿里与腾讯达成合作协议")));
        System.out.println((s.process("预估值180亿 首钢股份获注迁安钢铁")));
        System.out.println((s.process("“试玩互动”获超过1千万元天使轮融资，游戏广告可“先试玩再下载”")));
        System.out.println((s.process("这之后，百度还发布了好运中国年、AI办事处等十多个即将在2020年推出的超级IP，对用户的精细化运营正在为百度创造更大的商业空间。")));
        System.out.println((s.process("首发|借助AI智能升级传统餐饮业 艾唯尔科技获东方富海数千万元Pre-A轮融资")));
        System.out.println((s.process("百度是全球最大的中文搜索引擎，2000年1月由李彦宏、徐勇两人创立于北京中关村，致力于向人们提供“简单，可依赖”的信息获取方式")));
        System.out.println((s.process("据了解，三七互娱旗下首款云游戏已在开发当中，未来将登陆华为云游戏平台。")));
        System.out.println((s.process("云计算将推动物联网产业发展")));
        System.out.println((s.process("字节跳动与腾讯达成合作协议")));
        System.out.println((s.process("嗅问将是上海文磨网络科技开发的一款信息整合产品")));
        System.out.println((s.process("呼吸道飞沫传染是新型冠状病毒的主要传播途径")));
        System.out.println((s.process("新款讴歌TL登上环保目录")));
    }

}
