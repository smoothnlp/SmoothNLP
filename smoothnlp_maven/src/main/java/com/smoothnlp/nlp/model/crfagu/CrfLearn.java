package com.smoothnlp.nlp.model.crfagu;

import org.apache.commons.cli.*;

/**
 * Created by zhifac on 2017/4/3.
 */
public class CrfLearn {
    public static boolean run(String[] args) {
        // to-do  embedding file ;
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("f", "freq", true, "use features that occuer no less than INT(default 1)");
        options.addOption("m", "maxiter", true, "set INT for max iterations in LBFGS routine(default 10k)");
        options.addOption("c", "cost", true, "set FLOAT for cost parameter(default 1.0)");
        options.addOption("e", "eta", true, "set FLOAT for termination criterion(default 0.0001)");
        options.addOption("C", "convert", false, "convert text model to binary model");
        options.addOption("T", "convert_to_text", false, "convert binary model to text model");
        options.addOption("t", "textmodel", false, "build also text model file for debugging");
        options.addOption("a", "algorithm", true, "(CRF|CRF-L1|CRF-L2|MIRA)\", \"select training algorithm");
        options.addOption("p", "thread", true, "number of threads(default auto detect)");
        options.addOption("H", "shrinking-size", true, "set INT for number of iterations variable needs to be optimal before considered for shrinking. (default 20)");
        options.addOption("o","embed-defvalue-mode", true, "support default embedding vector value mode, support MAX ,AVG or zero.(default AVG)");
        options.addOption("h", "help", false, "show this help and exit");

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch(ParseException e) {
            printHelp(options);
            return false;
        }

        boolean convert = cmd.hasOption("C");
        boolean convertToText = cmd.hasOption("T");

        String[] restArgs = cmd.getArgs();

        if (cmd.hasOption("h") || ((convertToText || convert) && restArgs.length != 2) ||
            (!convert && !convertToText && restArgs.length != 4)) {
            printHelp(options);
            return cmd.hasOption("h");
        }
        int freq = Integer.valueOf(cmd.getOptionValue("f", "1"));
        int maxiter = Integer.valueOf(cmd.getOptionValue("m", "100000"));
        double C = Double.valueOf(cmd.getOptionValue("c", "1.0"));
        double eta = Double.valueOf(cmd.getOptionValue("e", "0.0001"));
        boolean textmodel = cmd.hasOption("t");
        int threadNum = Integer.valueOf(cmd.getOptionValue("p", "0"));
        if (threadNum <= 0) {
            threadNum = Runtime.getRuntime().availableProcessors();
        }
        int shrinkingSize = Integer.valueOf(cmd.getOptionValue("H", "20"));

        String algorithm = cmd.getOptionValue("a", "CRF-L2");
        algorithm = algorithm.toLowerCase();
        Encoder.Algorithm algo = Encoder.Algorithm.CRF_L2;
        if (algorithm.equals("crf") || algorithm.equals("crf-l2")) {
            algo = Encoder.Algorithm.CRF_L2;
        } else if (algorithm.equals("crf-l1")) {
            algo = Encoder.Algorithm.CRF_L1;
        } else if (algorithm.equals("mira")) {
            algo = Encoder.Algorithm.MIRA;
        } else {
            System.err.println("unknown algorithm: " + algorithm);
            return false;
        }

        String embDefMode = cmd.getOptionValue('o', "AVG");

        if (convert) {
            EncoderFeatureIndex featureIndex = new EncoderFeatureIndex(1);
            if (!featureIndex.convert(restArgs[0], restArgs[1])) {
                System.err.println("fail to convert text model");
                return false;
            }
        } else if (convertToText) {
            DecoderFeatureIndex featureIndex = new DecoderFeatureIndex();
            if (!featureIndex.convert(restArgs[0], restArgs[1])) {
                System.err.println("fail to convert binary model");
                return false;
            }
        } else {
            Encoder encoder = new Encoder();
            System.out.println("start learning...");
            if (!encoder.learn(restArgs[0], restArgs[1], restArgs[2], restArgs[3],
                textmodel, maxiter, freq, eta, C, threadNum, shrinkingSize, algo, embDefMode)) {
                System.err.println("fail to learn model");
                return false;
            }
        }
        return true;
    }

    public static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java " + CrfLearn.class.getName() + " [options] files", options);
    }

    public static void main(String[] args) {
        CrfLearn.run(args);
    }
}
