package com.smoothnlp.nlp.utility;

/**
 * math tools
 */
public class MathUtility {
    public static float sum(float ... var)
    {
        float sum = 0;
        for (float x : var)
        {
            sum += x;
        }

        return sum;
    }
}
