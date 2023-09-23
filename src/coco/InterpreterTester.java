package coco;

import java.io.*;

import org.apache.commons.cli.*;

public class InterpreterTester {

    public static void main (String[] args) {
        Options options = new Options();
        options.addRequiredOption("s", "src", true, "Source File");
        options.addOption("i", "in", true, "Data File");

        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser cmdParser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = cmdParser.parse(options, args);
        } catch (ParseException e) {
            formatter.printHelp("All Options", options);
            System.exit(-1);
        }

        Scanner s = null;
        String sourceFile = cmd.getOptionValue("src");
        try {
            s = new Scanner(new FileReader(sourceFile));
        }
        catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error accessing the code file: \"" + sourceFile + "\"");
            System.exit(-3);
        }

        InputStream in = System.in;
        if (cmd.hasOption("in")) {
            String inputFilename = cmd.getOptionValue("in");
            try {
                in = new FileInputStream(inputFilename);
            }

            catch (IOException e) {
                System.err.println("Error accessing the data file: \"" + inputFilename + "\"");
                System.exit(-2);
            }
        }
        else {
            String infile =  sourceFile.replace(".txt", ".in");
            try {
                in = new FileInputStream( infile );
            } catch (FileNotFoundException e) {
                in = System.in;
            }
        }
        
        Interpreter p = new Interpreter(s, in);
        p.interpret();
        if (p.hasError()) {
            System.out.println("Error parsing file");
            System.out.println(p.errorReport());
            System.exit(-4);
        }
    }
}
