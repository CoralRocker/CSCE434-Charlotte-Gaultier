package coco;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.apache.commons.cli.*;

public class CompilerTester {

    public static void main(String[] args) {
        Options options = new Options();
        options.addRequiredOption("s", "src", true, "Source File");
        options.addOption("i", "in", true, "Data File");
        options.addOption("nr", "reg", true, "Num Regs");
        options.addOption("b", "asm", false, "Print DLX instructions");
        options.addOption("a", "astOut", false, "Print AST to screen");
        options.addOption("d", "diff-ast", false, "If output file is present, get diff between the two ASTs");

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

        if( cmd.hasOption("d") ) {
            String[] userAst = ast_text.split(System.lineSeparator());
            ArrayList<String> testAst = new ArrayList<>();

            String testName = sourceFile.replace(".txt", "_ast.txt");
            BufferedReader testFile;

            try {
                testFile = new BufferedReader( new FileReader( testName ) );
                String line = null;
                while( (line = testFile.readLine()) != null ) {
                    testAst.add(line);
                }

            } catch (FileNotFoundException e) {
                System.err.printf("File %s does not exist\n", testName );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if( ! testAst.isEmpty() ) {
                for( int i = 0; i < userAst.length; i++ ) {
                    if( i >= testAst.size() ) {
                        System.err.printf("Line %d: Test AST EOF\n", i+1);
                        return;
                    }
                    else if( !Objects.equals(userAst[i], testAst.get(i)) ) {
                        System.err.printf("Line %d: Test AST != User AST\n", i+1);
                        System.err.printf("\"%s\" != \"%s\"\n", testAst.get(i), userAst[i] );
                        return;
                    }
                }

                if( testAst.size() > userAst.length ) {
                    System.err.printf("Line %d: Test AST has more lines than User AST\n", userAst.length+1);
                    for( int i = userAst.length; i < testAst.size(); i++ ) {
                        System.err.printf("\t%d: \"%s\"\n", i+1, testAst.get(i));
                    }
                    return;
                }

                System.out.printf("Test AST & User AST are equal\n");
            }
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
