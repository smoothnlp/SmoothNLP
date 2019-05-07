package com.smoothnlp.nlp.model.hmm;

import org.apache.commons.cli.*;

public class HmmLearn {
    public static boolean run(String[] args){
        CommandLineParser  parser= new DefaultParser();
        Options options = new Options();
        options.addOption("f", "freq", true, "use features that occuer no less than INT(default 1)");
        options.addOption("m", "maxiter", true, "set INT for max iterations in LBFGS routine(default 10k)");
        options.addOption("c", "cost", true, "set FLOAT for cost parameter(default 1.0)");
        options.addOption("e", "eta", true, "set FLOAT for termination criterion(default 0.0001)");
        options.addOption("C", "convert", false, "convert text model to binary model");
        options.addOption("T", "convert_to_text", false, "convert binary model to text model");
        options.addOption("t", "textmodel", false, "build also text model file for debugging");
        options.addOption("a", "algorithm", true, "(FirHMM|SecHMM)\", \"select training algorithm");
        options.addOption("p", "thread", true, "number of threads(default auto detect)");
        options.addOption("H", "shrinking-size", true, "set INT for number of iterations variable needs to be optimal before considered for shrinking. (default 20)");
        options.addOption("h", "help", false, "show this help and exit");

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch(ParseException e) {
            printHelp(options);
            return false;
        }

        if (cmd.hasOption("h")) {
            printHelp(options);
            return cmd.hasOption("h");
        }

        String algorithm = cmd.getOptionValue("a", "FirHMM");
        algorithm = algorithm.toLowerCase();
        HmmEncoder.Algorithm algo = HmmEncoder.Algorithm.FirstOrderHMM;
        if  (algorithm.equals("sechmm")){
            algo = HmmEncoder.Algorithm.FirstOrderHMM;
        }
        return false;

    }

    public static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java " + HmmLearn.class.getName() + " [options] files", options);
    }
}
