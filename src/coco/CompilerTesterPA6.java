package coco;

import ir.cfg.CFG;
import ir.cfg.optimizations.ReachingDefinition;
import org.apache.commons.cli.*;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;


public class CompilerTesterPA6 {

    public static void main(String[] args) {
        Options options = new Options();
        options.addRequiredOption("s", "src", true, "Source File");
        options.addOption("i", "in", true, "Data File");
        options.addOption("nr", "reg", true, "Num Regs");
        options.addOption("b", "asm", false, "Print DLX instructions");
        options.addOption("a", "astOut", false, "Print AST");

        options.addOption("gDir", "graphDir", false, "Graph dir, default will be current dir");
        options.addOption("ast", "ast", false, "Print AST.dot - requires coco.graphs/");



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

        if( cmd.hasOption('a') ) {
            System.out.println(ast.printPreOrder());
        }

        types.TypeChecker tc = new types.TypeChecker();
        tc.check(ast);



        if (tc.hasError()) {
            System.out.println("Error type-checking file.");
            System.out.println(tc.errorReport());
            System.exit(-4);
        }

//        List<CFG> cfgs = c.genSSA(ast);
//        Iterator<CFG> iterator = cfgs.iterator();
//        while (iterator.hasNext()) {
//            CFG ssa = iterator.next();
//            ssa.calculateDOMSets();
//            System.out.println(ssa.asDotGraph());
//        }
        c.genSSA(ast).asDotGraph();

        for( int i = 0; i < 2; i++ ) {
            System.out.printf("Running GCP + CF: %2d\n", i);
            ReachingDefinition gcp = new ReachingDefinition(c.genSSA(ast), true, true, false, true);
            System.out.println(c.genSSA(ast).asDotGraph());
        }
    }
}