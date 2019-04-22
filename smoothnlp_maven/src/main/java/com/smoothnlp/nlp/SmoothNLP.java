package com.smoothnlp.nlp;

import java.util.logging.Logger;
import com.smoothnlp.nlp.io.*;



public class SmoothNLP {

    public static String NAME = "SmoothNLP";

    public static Logger LOGGER = Logger.getLogger("SmoothNLP");

    public static IIOAdapter IOAdaptor = new FileIOAdapter();

    public static String CRF_SEGMENT_MODEL = "segment_crfpp.bin";

    public static String CRF_POSTAG_MODEL = "postag_crfpp.bin";


}
