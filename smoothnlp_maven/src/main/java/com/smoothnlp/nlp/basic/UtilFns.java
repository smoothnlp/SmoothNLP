package com.smoothnlp.nlp.basic;

import java.util.Arrays;

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

    public static float[] flatten2dFloatArray(Float[][] f2darray){
        float output[] = new float[f2darray.length*f2darray[0].length];
        int counter = 0;
        for (Float[] farray:f2darray){
            for (Float f: farray){
                output[counter]=(f != null ? f : Float.NaN);
                counter+=1;
            }
        }
        return output;
    }

    public static float[] flatten2dFloatArray(float[][] f2darray){
        float output[] = new float[f2darray.length*f2darray[0].length];
        int counter = 0;
        for (float[] farray:f2darray){
            for (float f: farray){
                output[counter]=f;
                counter+=1;
            }
        }
        return output;
    }

}
