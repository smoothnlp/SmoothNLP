package com.smoothnlp.nlp.basic;

public class UtilFns {

    public static String join(String delimeter, Object[] contents){
        if (contents.length<=0){return "";}
        else{
            StringBuilder sb = new StringBuilder();
            for (int i =0; i < contents.length; i++){
                sb.append(contents[i].toString());
                sb.append(delimeter);
            }
            sb.setLength(sb.length()-1);
            return sb.toString();
        }
    }

}
