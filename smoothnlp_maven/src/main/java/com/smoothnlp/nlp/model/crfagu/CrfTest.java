package com.smoothnlp.nlp.model.crfagu;

import org.apache.commons.cli.*;

import java.io.*;

/**
 * Created by zhifac on 2017/4/3.
 */
public class CrfTest {
    public static boolean run(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addRequiredOption("m", "model", true, "set FILE for model file");
        options.addOption("n", "nbest", true, "output n-best results");
        options.addOption("v", "verbose", true, "set INT for verbose level");
        options.addOption("c", "cost-factor", true, "set cost factor");
        options.addOption("o", "output", true, "output file path");
        options.addOption("h", "help", false, "show this help and exit");

        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        }catch(ParseException e) {
            printHelp(options);
            return false;
        }
        if (cmd.hasOption("h")) {
            printHelp(options);
            return true;
        }
        int nbest = Integer.valueOf(cmd.getOptionValue("n", "0"));
        int vlevel = Integer.valueOf(cmd.getOptionValue("v", "0"));
        double costFactor = Double.valueOf(cmd.getOptionValue("c", "1.0"));
        String model = cmd.getOptionValue("m");
        String outputFile = cmd.getOptionValue("o", null);

        TaggerImpl tagger = new TaggerImpl(TaggerImpl.Mode.TEST);
        try {
            FileInputStream stream = new FileInputStream(model);
            if (!tagger.open(stream, nbest, vlevel, costFactor)) {
                System.err.println("open error");
                return false;
            }
            String[] restArgs = cmd.getArgs();
            if (restArgs.length == 0) {
                return false;
            }

            OutputStreamWriter osw = null;
            if (outputFile != null) {
                osw = new OutputStreamWriter(new FileOutputStream(outputFile));
            }
            for (String inputFile: restArgs) {
                FileInputStream fis = new FileInputStream(inputFile);
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                BufferedReader br = new BufferedReader(isr);

                while (true) {
                    TaggerImpl.ReadStatus status = tagger.read(br);
                    if (TaggerImpl.ReadStatus.ERROR == status) {
                        System.err.println("read error");
                        return false;
                    } else if (TaggerImpl.ReadStatus.EOF == status && tagger.empty()) {
                        break;
                    }
                    if (!tagger.parseEmbedding()) {
                        System.err.println("parse error");
                        return false;
                    }
                    if (osw == null) {
                        System.out.print(tagger.toString());
                    } else {
                        osw.write(tagger.toString());
                    }
                }
                if (osw != null) {
                    osw.flush();
                }
                br.close();
            }
            if (osw != null) {
                osw.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java " + CrfTest.class.getName() + " [options] files", options);
    }

    public static void main(String[] args) {
        CrfTest.run(args);
    }
}
