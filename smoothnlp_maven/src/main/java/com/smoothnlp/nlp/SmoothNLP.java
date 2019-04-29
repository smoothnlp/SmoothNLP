package com.smoothnlp.nlp;

import java.util.logging.Logger;
import com.smoothnlp.nlp.io.*;
import com.smoothnlp.nlp.pipeline.ISequenceTagger;
import com.smoothnlp.nlp.pipeline.*;
import com.smoothnlp.nlp.pipeline.dependency.IDependencyParser;
import com.smoothnlp.nlp.pipeline.dependency.MaxEdgeScoreDependencyParser;

public class SmoothNLP {

    public static String NAME = "SmoothNLP";

    public static Logger LOGGER = Logger.getLogger("SmoothNLP");

    public static IIOAdapter IOAdaptor = new ResourceIOAdapter();

    public static String CRF_SEGMENT_MODEL = "segment_crfpp.bin";

    public static String CRF_POSTAG_MODEL = "postag_crfpp.bin";

    public static String DP_EDGE_SCORE_XGBOOST = "DP_Edge_Score_XgbModel.bin";

    // static Pipelines
    public static ISequenceTagger SEGMENT_PIPELINE = new SegmentCRFPP();
    public static ISequenceTagger POSTAG_PIPELINE = new PostagCRFPP();
    public static IDependencyParser DEPENDENCY_PIPELINE = new MaxEdgeScoreDependencyParser();




}
