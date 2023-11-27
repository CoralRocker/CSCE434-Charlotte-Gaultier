package coco;

import ir.cfg.CFG;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

//PA 7 generates 2 digraphs, one un-optimized, one optimized.
public class CompilerTesterPA89 {

    public static void main(String[] args) {
        Options options = new Options();
        options.addRequiredOption("s", "src", true, "Source File");
        options.addOption("i", "in", true, "Data File");
        options.addOption("nr", "reg", true, "Num Regs");
        options.addOption("b", "asm", false, "Print DLX instructions");
        options.addOption("a", "astOut", false, "Print AST");

        options.addOption("d", "setDir", true, "Set Dir used to store graph");
        options.addOption("gDir", "graphDir", false, "Graph dir, default will be current dir");
        options.addOption("ast", "ast", false, "Print AST.dot - requires coco.graphs/");

        options.addOption("cfg", "cfg", false, "Print CFG.dot - requires coco.graphs/");
        options.addOption("onefile", "onefile", false, "If true, 'ast.dot' and 'cfg.dot' are the names for files in coco.graphs/");
        options.addOption("allowVersions", "allowVersions", false, "Allowing versioning for files in coco.graphs/");


        options.addOption("o", "opt", true, "Order-sensitive optimization -allowed to have multiple");
        options.addOption("max", "maxOpt", false, "Run all available optimizations till convergence");

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
        if (cmd.hasOption("astOut")) {
            System.out.println(ast_text);
        }

        // create graph dir if needed
        String graphDir = "graphs";
        if( cmd.hasOption('d') ) {
            graphDir = cmd.getOptionValue('d');
        }

        if (cmd.hasOption("graphDir")) {
//            graphDir = cmd.getOptionValue("graphDir");
            File dir = new File(graphDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(graphDir + "/hello.txt");
            try {
                boolean res = file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error accessing the ast file: ");
                System.exit(-2);
            }
        }


        if (cmd.hasOption("ast")) {
            String filename = cmd.hasOption("onefile") ? "ast.dot" : sourceFile.substring(0, sourceFile.lastIndexOf('.')) + "_ast.dot";
            try (PrintStream out = new PrintStream(graphDir+'/'+filename)) {
                out.print(ast.asDotGraph());
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error accessing the ast file: " + graphDir + File.pathSeparator + filename);
                System.exit(-2);
            }
        }

        types.TypeChecker tc = new types.TypeChecker();

        if (!tc.check(ast)) {
            System.out.println("Error type-checking file.");
            System.out.println(tc.errorReport());
            System.exit(-4);
        }

        String dotgraph_text = "";
        try {
            c.genSSA(ast).asDotGraph();
//            Iterator<CFG> iterator = c.genSSA(ast).iterator();
//            while (iterator.hasNext()) {
//                CFG curCFG = iterator.next();
//                dotgraph_text += curCFG.asDotGraph();
//            }
//            System.out.println(dotgraph_text);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error caught - see stderr for stack trace " + e.getMessage());
            System.exit(-5);
        }

        if (cmd.hasOption("cfg")) {
            String filename = cmd.hasOption("onefile") ? "cfg.dot" : sourceFile.substring(0, sourceFile.lastIndexOf('.')) + "_cfg.dot";
            try (PrintStream out = new PrintStream(graphDir+"/"+filename)) {
                out.print(dotgraph_text);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error accessing the cfg file: "+ graphDir + File.pathSeparator + filename);
                System.exit(-2);
            }
        }

        // Comment these out for PA 7 - 8 - 9
        String[] optArgs = cmd.getOptionValues("opt");
        List<String> optArguments = (optArgs!=null && optArgs.length != 0) ? Arrays.asList(optArgs) : new ArrayList<String>();

        //PA 7
        try {
            dotgraph_text = c.optimization(optArguments, cmd);
            System.out.println(dotgraph_text);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error caught - see stderr for stack trace " + e.getMessage());
            System.exit(-6);
        }


         // //PA 9
         int[] program = c.compile();
         if (c.hasError()) {
             System.err.println("Error compiling file");
             System.err.println(c.errorReport());
             System.exit(-4);
         }

        // if (cmd.hasOption("asm")) {

        //     String asmFile = sourceFile.substring(0, sourceFile.lastIndexOf('.')) + "_asm.txt";
        //     try (PrintStream out = new PrintStream(asmFile)) {
        //         for (int i = 0; i < program.length; i++) {
        //             out.print(i + ":\t" + DLX.instrString(program[i])); // \newline included in DLX.instrString()
        //         }
        //     } catch (IOException e) {
        //         System.err.println("Error accessing the asm file: \"" + asmFile + "\"");
        //         System.exit(-5);
        //     }
        // }

        DLX.load(program);
        try {
            DLX.execute(in);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("IOException inside DLX");
            System.exit(-6);
        }


    }
}
