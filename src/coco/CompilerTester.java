package coco;

import java.io.*;
import org.apache.commons.cli.*;

public class CompilerTester {

    public static void main(String[] args) {
        Options options = new Options();
        options.addRequiredOption("s", "src", true, "Source File");
        options.addOption("i", "in", true, "Data File");
        options.addOption("nr", "reg", true, "Num Regs");
        options.addOption("b", "asm", false, "Print DLX instructions");
        options.addOption("a", "astOut", false, "Print AST to screen");

        options.addOption("gDir", "graphDir", false, "Graph dir, default will be current dir");
        options.addOption("ast", "ast", false, "Print AST.txt - requires graphs/");


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
        } catch (IOException e) {
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

        String strNumRegs = cmd.getOptionValue("reg", "24");
        int numRegs = 24;
        try {
            numRegs = Integer.parseInt(strNumRegs);
            if (numRegs > 24) {
                System.err.println("reg num too large - setting to 24");
                numRegs = 24;
            }
            if (numRegs < 4) {
                System.err.println("reg num too small - setting to 4");
                numRegs = 4;
            }
        } catch (NumberFormatException e) {
            System.err.println("Error in option NumRegs -- reseting to 24 (default)");
            numRegs = 24;
        }

        
        Compiler c = new Compiler(s, numRegs);
        ast.AST ast = c.genAST();
        
        String ast_text = ast.printPreOrder();
        if (cmd.hasOption("a")) { // AST to Screen
            System.out.println(ast_text);
        }

        // create graph dir if needed
        String graphDir = "";
        if (cmd.hasOption("graphDir")) {
            graphDir = cmd.getOptionValue("graphDir");
            File dir = new File(graphDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }


        if (cmd.hasOption("ast")) {
            String astFile = sourceFile.substring(0, sourceFile.lastIndexOf('.')) + "_ast.txt";
            try (PrintStream out = new PrintStream(astFile)) {
                out.println(ast.printPreOrder());
            } catch (IOException e) {
                System.err.println("Error accessing the ast file: \"" + astFile + "\"");
                System.exit(-7);
            }
        }

        if (c.hasError()) {
            System.out.println("Error parsing file.");
            System.out.println(c.errorReport());
            System.exit(-8);
        }
    }
}
