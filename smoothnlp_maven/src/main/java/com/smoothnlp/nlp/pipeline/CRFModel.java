package com.smoothnlp.nlp.pipeline;


import com.smoothnlp.nlp.basic.SDictionarySupported;

public abstract class CRFModel extends BaseSequenceTagger {
    public String buildFtrs(char c, String[] ftrs){
        return buildFtrs(String.valueOf(c),ftrs);
    }

    public String buildFtrs(String token, String[] ftrs){
        return token+"\t"+ join("\t",ftrs);
    }

    public String buildFtrs(char c){return String.valueOf(c);}

    public String buildFtrs(String token){return token;}

    public static String join(String delimeter, String[] contents){
        if (contents.length<=0){return "";}
        else{
            StringBuilder sb = new StringBuilder();
            for (int i =0; i < contents.length; i++){
                sb.append(contents[i]);
                sb.append(delimeter);
            }
            sb.setLength(sb.length()-1);
            return sb.toString();
        }
    }




//    public static void main(String[] args){
//        System.out.println(join("\t",new String[]{"haha"}));
//    }

}
