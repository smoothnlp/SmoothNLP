package com.smoothnlp.nlp.pipeline;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.basic.SToken;
import com.smoothnlp.nlp.basic.SEntity;
import com.smoothnlp.nlp.basic.UtilFns;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class NormalizedNER extends BaseEntityRecognizer{

    public static String BACKGROUND_SYMBOL = "0";
    private static String LITERAL_DECIMAL_POINT = "点";

    private static final String MONEY_TAG = "MONEY";
    private static final String NUMBER_TAG = "NUMBER";
    private static final String PERCENT_TAG = "PERCENT";
    private static final String DATE_TAG = "DATE";

    private static final Set<String> quantifiable; // Entity types that are quantifiable
    private static final Set<String> moneyPostagSet;
    private static final Map<String, Character> multiCharCurrencyWords;
    private static final Map<String, Character> oneCharCurrencyWords;
    private static final Map<String, Double> wordsToValue;
    private static final Map<String, Double> quantityUnitToValues;
    private static final Map<String, String> fullDigitToHalfDigit;

    // Pattern we need

    private static final Pattern ARABIC_NUMBERS_PATTERN = Pattern.compile("[-+]?\\d*\\.?\\d+");
    private static final Pattern MIX_NUMBERS_PATTERN = Pattern.compile("[-+]?\\d*\\.?\\d+(亿|万)");
    private static final Pattern CHINESE_MIX_NUMBERS_PATTERN = Pattern.compile("[一二三四五六七八九零十〇\\d]+[点一二三四五六七八九零十〇\\d]*[千]*(亿|万)");
    private static final Pattern CHINESE_MIX_NUMBERS_PATTERN2 = Pattern.compile("[一二三四五六七八九零十〇\\d]+[点一二三四五六七八九零十〇\\d]*");

    public static final Pattern CURRENCY_WORD_PATTERN =
            Pattern.compile("元|元钱|刀|(?:美|欧|加|日|韩)元|英?磅|法郎|卢比|卢布|马克|先令|克朗|泰铢|(?:越南)?盾|美分|便士|块钱|毛钱|角钱");
    public static final Pattern PERCENT_WORD_PATTERN1 = Pattern.compile("(?:百分之|千分之).+");
    public static final Pattern PERCENT_WORD_PATTERN2 = Pattern.compile(".+%");

    private static final Pattern CHINESE_AND_ARABIC_NUMERALS_PATTERN = Pattern.compile("[一二三四五六七八九零十〇\\d]+");
    private static final String DATE_AGE_LOCALIZER = "后";

    // order it by number of characters DESC for hand one-by-one matching of string suffix
    public static final String[] CURRENCY_WORDS_VALUES = new String[]{"越南盾", "美元", "欧元", "日元", "韩元",
            "英镑", "澳元", "加元", "法郎", "卢比", "卢布", "马克", "先令", "克朗","泰铢", "盾", "刀", "镑", "元"};

    static {
        quantifiable = new HashSet<String>();
        quantifiable.add(NUMBER_TAG);
        quantifiable.add(MONEY_TAG);
        quantifiable.add(PERCENT_TAG);
        quantifiable.add(DATE_TAG);

        multiCharCurrencyWords  = new HashMap<String, Character>();
        multiCharCurrencyWords.put("美元", '$');
        multiCharCurrencyWords.put("美分", '$');
        multiCharCurrencyWords.put("英镑", '£');
        multiCharCurrencyWords.put("便士",'£');
        multiCharCurrencyWords.put("欧元",'£');
        multiCharCurrencyWords.put("日元",'¥');
        multiCharCurrencyWords.put("韩元",'₩');

        oneCharCurrencyWords = new HashMap<String, Character>();
        oneCharCurrencyWords.put("刀",'$');
        oneCharCurrencyWords.put("镑",'£');
        oneCharCurrencyWords.put("元",'¥');

        quantityUnitToValues = new HashMap<String, Double>();
        quantityUnitToValues.put("十",10.0);
        quantityUnitToValues.put("百",100.0);
        quantityUnitToValues.put("千",1000.0);
        quantityUnitToValues.put("万",10000.0);
        quantityUnitToValues.put("亿",100000000.0);

        wordsToValue = new HashMap<String,Double>();
        wordsToValue.put("零",0.0);
        wordsToValue.put("〇",0.0);
        wordsToValue.put("一",1.0);
        wordsToValue.put("二",2.0);
        wordsToValue.put("三",3.0);
        wordsToValue.put("四",4.0);
        wordsToValue.put("五",5.0);
        wordsToValue.put("六",6.0);
        wordsToValue.put("七",7.0);
        wordsToValue.put("八",8.0);
        wordsToValue.put("九",9.0);
        wordsToValue.putAll(quantityUnitToValues);

        fullDigitToHalfDigit = new HashMap<>();
        fullDigitToHalfDigit.put("1","1");
        fullDigitToHalfDigit.put("2","2");
        fullDigitToHalfDigit.put("3","3");
        fullDigitToHalfDigit.put("4","4");
        fullDigitToHalfDigit.put("5","5");
        fullDigitToHalfDigit.put("6","6");
        fullDigitToHalfDigit.put("7","7");
        fullDigitToHalfDigit.put("8","8");
        fullDigitToHalfDigit.put("9","9");
        fullDigitToHalfDigit.put("0","0");

        moneyPostagSet = new HashSet<>();
        moneyPostagSet.add("m");
        moneyPostagSet.add("CD");

    }

    public List<SEntity> classify(List<SToken> document){
        List<SEntity> entityList = new ArrayList<SEntity>();
        List<SToken> paddingList = new ArrayList<SToken>();
        paddingList.add(new SToken("", null));
        paddingList.addAll(document);
        paddingList.add(new SToken("", null));
        int charEnd = 0;
        for (int i = 1, sz = paddingList.size()-1; i < sz; i ++ ){
            SToken me = paddingList.get(i);
            SToken prev = paddingList.get(i-1);
            SToken next = paddingList.get(i+1);
            SEntity entity = new SEntity();
            entity.charStart = charEnd;
            charEnd += me.token.length();
            entity.charEnd = charEnd;
            entity.text = me.token;

            entity.sTokenList = new HashMap<Integer, SToken>();
            entity.sTokenList.put(i,me);
            entity.nerTag = null;
            //if (CURRENCY_WORD_PATTERN.matcher(me.token).matches() && prev.postag.equals("m")){
            if(CURRENCY_WORD_PATTERN.matcher(me.token).matches() && i != 1 && prev.postag.equals("m")) {
                entity.nerTag = MONEY_TAG;
//            }else if (me.postag.equals("m")){
            }else if (moneyPostagSet.contains(me.postag.toString())){

                if (PERCENT_WORD_PATTERN1.matcher(me.token).matches() || PERCENT_WORD_PATTERN2.matcher(me.token).matches()){
                    entity.nerTag = PERCENT_TAG;
                }else if(rightScanFindsMoneyWord(paddingList, i)){
                    entity.nerTag = MONEY_TAG;
                }else if(me.token.length() == 2 && CHINESE_AND_ARABIC_NUMERALS_PATTERN.matcher(me.token).matches() && DATE_AGE_LOCALIZER.equals(next.token)){
                    entity.nerTag = DATE_TAG;
                }else{
                    entity.nerTag = NUMBER_TAG;
                }
            }else if (PERCENT_WORD_PATTERN1.matcher(me.token).matches() || PERCENT_WORD_PATTERN2.matcher(me.token).matches()){
                entity.nerTag = PERCENT_TAG;
            }else //if (me.postag.equals("NN")){
                    if(MIX_NUMBERS_PATTERN.matcher(me.token).matches()|| CHINESE_MIX_NUMBERS_PATTERN.matcher(me.token).matches()||CHINESE_MIX_NUMBERS_PATTERN2.matcher(me.token).matches()){
                        entity.nerTag=NUMBER_TAG;
            }
            entityList.add(entity);

        }
        return entityList;
    }

    private static boolean rightScanFindsMoneyWord(List<SToken> pl, int i ){
        int j = i ;
        int sz = pl.size();
        while (j<sz & j<i+4){
            String word = pl.get(j).token;
            j++;
            if (CURRENCY_WORD_PATTERN.matcher(word).matches()){
                return true;
            }
        }
        if (j >= sz){
            return false;
        }
        return false;
        //return ((tag.equals("m")|| tag.equals("n") || tag.equals("nz")) && CURRENCY_WORD_PATTERN.matcher(word).matches());
        //return ((tag.equals("M")|| tag.equals("NN")||tag.equals("NNS")) && CURRENCY_WORD_PATTERN.matcher(word).matches());

    }


    public List<SEntity> process(String inputText){
        List<SToken> termList = SmoothNLP.POSTAG_PIPELINE.process(inputText);
        return process(termList);
    }

    /**
     * Indentifies contiguous MONEY, TIME , DATE or PERCENT entities
     * and tags each of their constituents with a 'normalizedQuantity"
     * label which contains the appropriate normalized string corresponding to
     * the full quantity.
     * Unlike English normalizer, this method currently does not support
     * concatenation .
     * @param termList
     * @return
     */

    public List<SEntity> process(List<SToken> termList){
       // classify terms to entity.nerTags
        List<SEntity> entityList = classify(termList);

        String prevNerTag = BACKGROUND_SYMBOL;

        ArrayList <SEntity> collector = new ArrayList<SEntity>();
        for (int i = 0, sz = entityList.size(); i<=sz;i++){
            SEntity currEntity = null;
            SEntity nextEntity = null;
            String currNerTag = null;
            String nextWord = "";
            if(i < sz){
                currEntity = entityList.get(i);
                if(i+1 < sz){

                    nextEntity = entityList.get(i+1);
                    nextWord = nextEntity.text;
                    if (nextWord == null) {
                        nextWord = "";
                    }
                }
                currNerTag = currEntity.nerTag;
            }

            if (prevNerTag!= null && currNerTag!= null && currNerTag.equals(MONEY_TAG) && prevNerTag.equals(NUMBER_TAG)){
                if(collector.get(collector.size()-1).charEnd == currEntity.charStart){
                    collector.add(currEntity);
                    processEntity(collector, currNerTag,null, nextWord, termList);
                }
                i ++ ;
                collector = new ArrayList<>();
            }else if((currNerTag == null)|| !currNerTag.equals(prevNerTag) && quantifiable.contains(prevNerTag)){
                if(prevNerTag != null){
                    switch (prevNerTag){
                        case DATE_TAG:
                            processEntity(collector, prevNerTag, null, nextWord, termList);
                            default:
                                if(!collector.isEmpty()){
                                    processEntity(collector, prevNerTag, null, nextWord,termList);
                                }
                                break;
                    }
                }
                collector = new ArrayList<>();
            }

            if(quantifiable.contains(currNerTag)){
                collector.add(currEntity);
            }
            prevNerTag = currNerTag;

        }

        Iterator<SEntity> iter = entityList.iterator();
        while (iter.hasNext()){
            SEntity sEntity = iter.next();
            if(sEntity.nerTag == null){
                iter.remove();
            }else if (sEntity.normalizedEntityValue==null){
                sEntity.normalizedEntityValue=sEntity.text;
            }
        }
        return entityList;
    }

    public static List<SEntity> processEntity(List<SEntity> l, String entityType, String compModifier,
                                             String nextWord, List<SToken> termslit){
        String s = singleEntityToString(l);
        StringBuilder sb = new StringBuilder();

        for (int i=0,sz = s.length(); i < sz; i ++ ){
            String ch = s.substring(i, i+1);
            if (fullDigitToHalfDigit.containsKey(ch)){
                ch = fullDigitToHalfDigit.get(ch);
            }
            sb.append(ch);
        }
        s = sb.toString();

        String p = null;

        switch (entityType){
            case NUMBER_TAG:
                p = "";
                if (compModifier != null){
                    p = compModifier;
                }
                String q = normalizedNumberString(s, nextWord, 1.0);
                if (q != null) {
                    p = p.concat(q);
                }else{
                    p = null;
                }
                break;
            case PERCENT_TAG:
                p = normalizedPercentString(s,nextWord);
                break;
            case MONEY_TAG:
                p = "";
                if (compModifier != null){
                    p = compModifier;
                }
                q = normalizedMoneyString(s, nextWord);
                if(q != null){
                    p = p.concat(q);
                }else{
                    p = null;
                }
                break;
        }
        int sz= l.size();
        SEntity lastEntity = l.get(sz-1);
        lastEntity.normalizedEntityValue = p;

        if(sz > 1){
            for(SEntity sEntity:l.subList(0, sz-1)){
                lastEntity.sTokenList.putAll(sEntity.sTokenList);
                sEntity.nerTag = null;
            }
            lastEntity.charStart = l.get(0).charStart;
            lastEntity.text = lastEntity.getText();
        }
        return l;

    }

    private static String normalizedMoneyString(String s, String nextWord){

        double multiplier = 1.0;
        boolean notMatched = true;
        char currencySign = '$';

        // we check multiCharCurrencyWords first
        for (String currencyWord: multiCharCurrencyWords.keySet()){
            if(notMatched && Pattern.compile(currencyWord).matcher(s).find()){
                switch (currencyWord){
                    case "美分":
                        multiplier = 0.01;
                        break;
                    case "先令":
                        multiplier = 0.05;
                        break;
                    case "便士":
                        multiplier = 1.0/240;
                        break;
                }
                s = s.replaceAll(currencyWord, "");
                currencySign = multiCharCurrencyWords.get(currencyWord);
                notMatched = false;
            }
        }

        // Then we check oneCharCurrencyWords
        if(notMatched){
            for (String currencyWord: oneCharCurrencyWords.keySet()){
                if(notMatched && Pattern.compile(currencyWord).matcher(s).find()){
                    s = s.replaceAll(currencyWord, "");
                    currencySign = oneCharCurrencyWords.get(currencyWord);
                    notMatched = false;
                }
            }
        }
        // we check all other currency case if we miss both dicitionaries above
        if (notMatched){
            for(String currencyWord: CURRENCY_WORDS_VALUES){
                if (notMatched && Pattern.compile(currencyWord).matcher(s).find()){
                    s = s.replaceAll(currencyWord, "");
                    break;
                }
            }
        }
        String value = normalizedNumberString(s,nextWord,multiplier);
        if (value == null){
            return null;
        }
        else{
            return currencySign + value ;
        }
    }

    /**
     *  normalized a string into the corresponding starndard number values (in String form).
     *  This method ca handle only a string of pure numerical expressions,
     *  like "两万三千零七十二点五六" or "23072.56". Other Ners like MONEY or DATE needs to be
     *  handled in their own methods.
     *  In any case fail ,this method will just return null.
     */
    private static String normalizedNumberString(String s ,String nextWord, double multiplier){
        // remove unnecessary characters in the String.
        s = s.trim();
        s = s.replaceAll("[ \t\n\0\f\r,]", "");
        if(s.isEmpty()) {
            return null;
        }
        // In case of pure arabic numbers, return the straight value of it.
        if(ARABIC_NUMBERS_PATTERN.matcher(s).matches()){
            return prettyNumber(String.format("%f", multiplier * Double.valueOf(s)));
        }
        // If this is not all arabic, we assume it to be either Chinese literal or mix of Chinese literal and arabic.
        //We handle decimal point first
        int decimalIndex = s.indexOf(LITERAL_DECIMAL_POINT);
        Double decimalValue = Double.valueOf(0);
        if(decimalIndex != -1){
            decimalValue = normalizeLiteralDecimalString(s.substring(decimalIndex+1));
            if(decimalValue == null){
                return null;
            }
            s = s.substring(0, decimalIndex);

        }
        Double integerValue = recurNormalizeLiteralIntegerString(s);
        if(integerValue == null){
            return null;
        }
        return prettyNumber(String.format("%f", multiplier * Double.valueOf(integerValue.doubleValue() + decimalValue.doubleValue())));
    }

    private static String prettyNumber(String s){
        if (s == null){
            return null;
        }
        s = ! s.contains(".") ? s : s.replaceAll("0*$", "").replaceAll("\\.$","");
        return s ;
    }

    private static Double normalizeLiteralDecimalString(String s){
        if (s.isEmpty()){
            return Double.valueOf(0);
        }

        if(!CHINESE_AND_ARABIC_NUMERALS_PATTERN.matcher(s).matches()){
            return null;
        }
        double decimalValue = 0 ;
        double base = 1 ;
        for (int i=0, sz = s.length(); i<sz; i++){
            base *= 0.1;
            String c = Character.toString(s.charAt(i));
            if(!wordsToValue.containsKey(c)){
                return null;
            }
            double v = wordsToValue.get(c);
            decimalValue += v * base;
        }
        return Double.valueOf(decimalValue);
    }

    private static Double recurNormalizeLiteralIntegerString(String s){
        if(s.isEmpty()){
            return Double.valueOf(0);
        }
        if(ARABIC_NUMBERS_PATTERN.matcher(s).matches()){
            return Double.valueOf(s);
        }

        if(s.length() > 1 && s.startsWith("零") || s.startsWith("〇")){
            s = s.substring(1);
        }
        if(s.length() == 1 && wordsToValue.containsKey(s)){
            return Double.valueOf(wordsToValue.get(s));
        }

        // parse the integer, making use of the compositionanlity of Chinese literal numbers
        Double value;
        value = compositeAtUnitIfExits(s,"亿");
        if (value!=null){
            return value;
        }else{
            value = compositeAtUnitIfExits(s, "万");
        }
        if(value!=null){
            return value;
        }else{
            value = compositeAtUnitIfExits(s, "千");
        }
        if(value!=null){
            return value;
        }else{
            value = compositeAtUnitIfExits(s, "百");
        }
        if(value!=null){
            return value;
        }else{
            value = compositeAtUnitIfExits(s, "十");
        }
        if(value != null){
            return value;
        }
        return null;
    }

    private static Double compositeAtUnitIfExits(String s ,String unit){
        if(!quantityUnitToValues.containsKey(unit)){
            return null;
        }
        int idx = s.indexOf(unit);
        if (idx != -1){
            Double first = Double.valueOf(1.0);
            if (("十".equals(unit) || "百".equals(unit)) && idx == 0) {
                // do nothing
            }else{
                first = recurNormalizeLiteralIntegerString(s.substring(0,idx));
            }
            Double second = recurNormalizeLiteralIntegerString(s.substring(idx+1));

            if (first != null && second != null){
                return Double.valueOf(first.doubleValue() * quantityUnitToValues.get(unit) + second.doubleValue());
            }
        }
        return null;
    }


    /**
     * Normalize a percent string. Handle both  % and ‰.
     */

    private static String normalizedPercentString(String s, String nextWord){

        String ns = "";
        if (s.startsWith("百分之")){
            ns = normalizedNumberString(s.substring(3),nextWord,1.0);
            if(ns!=null){
                ns+="%";
            }
        }else if (s.startsWith("千分之")){
            ns = normalizedNumberString(s.substring(3),nextWord, 1.0);
            if(ns!=null){
                ns+="‰";
            }
        }else if (s.endsWith("%")){
            ns = normalizedNumberString(s.substring(0, s.length()-1) ,nextWord, 1.0);
            if(ns!=null){
                ns+="%";
            }
        }else if (s.endsWith("‰")){
            ns = normalizedNumberString(s.substring(0, s.length()-1),nextWord, 1.0);
            if(ns!=null){
                ns+="‰";
            }
        }else{
            ns = normalizedNumberString(s,nextWord,1.0);
            if(ns!=null){
                ns+="%";
            }
        }
        return ns;
    }


    /**
     * Concatenate entity annotations to a String. Note that Chinese does not use space to separate
     * tokens so we will follow this convention here.
     * @param l
     * @return
     */
    private static String singleEntityToString(List<SEntity> l){
        StringBuilder sb = new StringBuilder();
        for (SEntity w : l){
            sb.append(w.text);
        }
        return sb.toString();
    }

    public ArrayList<HashMap<String,String>> getNormalizedNER(List<SToken> termList){
        /**
         * this is a deprecated function for version 0.1, implemented for convenience
         */


        List<SEntity> entityList = process(termList);

        ArrayList<HashMap<String,String>> nerRes = new ArrayList<HashMap<String, String>>();
        for(SEntity entity :entityList){
            HashMap<String, String> emMap = new HashMap<String, String>();
            emMap.put("text", entity.text);
            emMap.put("tokens",entity.sTokenList.toString());
            emMap.put("charStart", Integer.toString(entity.charStart));
            emMap.put("charEnd", Integer.toString(entity.charEnd));
            emMap.put("entityType", entity.nerTag);
            emMap.put("normlizedNamedEntityTag", entity.normalizedEntityValue);
            nerRes.add(emMap);
        }
        return  nerRes;
    }

    public ArrayList<HashMap<String,String>> getNormalizedNER(String inputText){
        return getNormalizedNER(SmoothNLP.POSTAG_PIPELINE.process(inputText));
    }

    public String analyze(String inputText){
        Gson gsonobject = new Gson();
        return gsonobject.toJson(getNormalizedNER(inputText));

    }

    public static void main(String[] args){
        String inputText;
        NormalizedNER ner = new NormalizedNER();
//        inputText = "我买了五斤苹果，总共10元";
//        System.out.println(SmoothNLP.POSTAG_PIPELINE.process(inputText));
//        System.out.println(ner.analyze(inputText));
//
//        inputText = "我一共带去30元，占百分之五十";
//        System.out.println(SmoothNLP.POSTAG_PIPELINE.process(inputText));
//        System.out.println(ner.analyze(inputText));


        inputText = "广汽集团一季度营收142.56亿，归母净利润27.78亿元";
        System.out.println(SmoothNLP.POSTAG_PIPELINE.process(inputText));
        System.out.println(ner.analyze(inputText));

        inputText = "数亿";
        System.out.println(SmoothNLP.POSTAG_PIPELINE.process(inputText));
        System.out.println(ner.analyze(inputText));

        inputText = "一点三亿";
        System.out.println(SmoothNLP.POSTAG_PIPELINE.process(inputText));
        System.out.println(ner.analyze(inputText));

        inputText = "江山控股(00295)拟11.66元出售10个太阳能项目";
        System.out.println(SmoothNLP.POSTAG_PIPELINE.process(inputText));
        System.out.println(ner.analyze(inputText));

        System.out.println(UtilFns.toJson(ner.process(inputText)));



    }
}
