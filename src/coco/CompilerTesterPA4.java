package coco;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ast.RootAST;
import org.apache.commons.cli.*;
import types.TypeChecker;

import static java.lang.System.exit;

public class CompilerTesterPA4 {

    public static void main(String[] args) {
        Options options = new Options();
        options.addRequiredOption("s", "src", true, "Source File");
        options.addOption("i", "in", true, "Data File");
        options.addOption("nr", "reg", true, "Num Regs");
        options.addOption("b", "asm", false, "Print DLX instructions");
        options.addOption("a", "astOut", false, "Print AST to screen");
        options.addOption("d", "diff-ast", false, "If output file is present, get diff between the two ASTs");
        options.addOption(null, "run-all", false, "Search current director for all \"testXXX.txt\" files");

        options.addOption("gDir", "graphDir", false, "Graph dir, default will be current dir");
        options.addOption("ast", "ast", false, "Print AST.txt - requires coco.graphs/");
        options.addOption("sym", "sym", false, "Print Symbol Table to screen");


        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser cmdParser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = cmdParser.parse(options, args);
        } catch (ParseException e) {
            formatter.printHelp("All Options", options);
            exit(-1);
        }

        ArrayList<String> files = new ArrayList<>();

        System.setErr(System.out);

        if (cmd.hasOption("run-all")) {
            File curDir = new File(".");
            Pattern ptrn = Pattern.compile("test\\d\\d\\d.txt");
            for (File entry : curDir.listFiles()) {
                if (entry.isFile()) {
                    String name = entry.getName();
                    Matcher matcher = ptrn.matcher(name);

                    if (matcher.matches()) {
                        files.add(name);
                    }
                }
            }

            Collections.sort(files);
            System.out.printf("All available files: %s\n", files);
        }
        else {
            files.add(cmd.getOptionValue("src"));
        }

        SourceLoop:
        for (String sourceFile : files) {

            System.out.printf("\n\nRunning: %s\n", sourceFile);

            Scanner s = null;
            try {
                s = new Scanner(new FileReader(sourceFile));
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error accessing the code file: \"" + sourceFile + "\"");
                exit(-3);
            }

            InputStream in = System.in;
            if (cmd.hasOption("in")) {
                String inputFilename = cmd.getOptionValue("in");
                try {
                    in = new FileInputStream(inputFilename);
                } catch (IOException e) {
                    System.err.println("Error accessing the data file: \"" + inputFilename + "\"");
                    exit(-2);
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
            RootAST ast = (RootAST) c.genAST();
            types.TypeChecker checker = new TypeChecker();
            checker.check(ast);

            if( checker.hasError() ) {
                System.err.println("Error type-checking file.");
                System.err.println(checker.errorReport());
            }

            String ast_text = ast.printPreOrder();
            if (cmd.hasOption("a") && !cmd.hasOption("run-all")) { // AST to Screen
                System.out.println(ast_text);
            }

            if (cmd.hasOption("sym")) { // Symbol Table to Screen
                System.out.println(c.symbolTable());
            }

            if (cmd.hasOption("d")) {
                String[] userAst = ast_text.split(System.lineSeparator());
                ArrayList<String> testAst = new ArrayList<>();

                String testName = sourceFile.replace(".txt", "_ast.txt");
                BufferedReader testFile;

                try {
                    testFile = new BufferedReader(new FileReader(testName));
                    String line = null;
                    while ((line = testFile.readLine()) != null) {
                        testAst.add(line);
                    }

                } catch (FileNotFoundException e) {
                    System.err.printf("File %s does not exist\n", testName);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (!testAst.isEmpty()) {
                    for (int i = 0; i < userAst.length; i++) {
                        if (i >= testAst.size()) {
                            System.err.printf("Line %d: Test AST EOF\n", i + 1);
                            continue SourceLoop;
                        } else if (!Objects.equals(userAst[i], testAst.get(i))) {
                            System.err.printf("Line %d: Test AST != User AST\n", i + 1);
                            System.err.printf("\"%s\" != \"%s\"\n", testAst.get(i), userAst[i]);
                            System.err.println("Full AST: ");
                            int idx = 1;
                            for( String line : userAst ) {
                                System.err.printf("\t%3d : \"%s\"\n", idx++, line);
                            }
                            continue SourceLoop;
                        }
                    }

                    if (testAst.size() > userAst.length) {
                        System.err.printf("Line %d: Test AST has more lines than User AST\n", userAst.length + 1);
                        for (int i = userAst.length; i < testAst.size(); i++) {
                            System.err.printf("\t%3d: \"%s\"\n", i + 1, testAst.get(i));
                        }
                        System.err.println("Full User AST: ");
                        int idx = 1;
                        for( String line : userAst ) {
                            System.err.printf("\t%3d: \"%s\"\n", idx++, line);
                        }
                        continue SourceLoop;
                    }

                    System.out.printf("Test AST & User AST are equal\n");
                }

                testName = sourceFile.replace(".txt", ".out");
                ArrayList<String> userErr = new ArrayList<>(List.of(c.errorReport().split(System.lineSeparator())));
                if( userErr.get(userErr.size()-1).isEmpty() ) {
                    userErr.remove(userErr.size()-1);
                }
                if( !userErr.isEmpty() ) {
                    userErr.add(0, "Error parsing file.");
                }

                userErr.addAll(List.of(checker.errorReport().split(System.lineSeparator())));
                if( !userErr.isEmpty() ) {
                    userErr.add(0, "Error type-checking file.");
                }



                ArrayList<String> testErr = new ArrayList<>();
                try {
                    testFile = new BufferedReader(new FileReader(testName));
                    String line = null;
                    while ((line = testFile.readLine()) != null) {
                        testErr.add(line);
                    }
                } catch (FileNotFoundException e) {
                    System.err.printf("File %s does not exist.\n", testName);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (!testErr.isEmpty()) {
                    for (int i = 0; i < userErr.size(); i++) {
                        if (i >= testErr.size()) {
                            System.err.printf("Line %d: Test ERR EOF\n", i + 1);
                            continue SourceLoop;
                        } else if (!Objects.equals(userErr.get(i), testErr.get(i))) {
                            System.err.printf("Line %d: Test ERR != User ERR\n", i + 1);
                            System.err.printf("\"%s\" != \"%s\"\n", testErr.get(i), userErr.get(i));
                            System.err.println("Full User ERR:");
                            int idx = 1;
                            for( String line : userErr ) {
                                System.err.printf("\t%3d: \"%s\"\n", idx++, line);
                            }
                            idx = 0;
                            System.err.println("Full Test ERR:");
                            for( String line : testErr ) {
                                System.err.printf("\t%3d: \"%s\"\n", idx++, line);
                            }

                            continue SourceLoop;
                        }
                    }

                    if (testErr.size() > userErr.size()) {
                        System.err.printf("Line %d: Test ERR has more lines than User ERR\n", userErr.size() + 1);
                        for (int i = userErr.size(); i < testErr.size(); i++) {
                            System.err.printf("\t%d: \"%s\"\n", i + 1, testErr.get(i));
                        }
                        continue SourceLoop;
                    }

                    System.out.printf("Test ERR & User ERR are equal\n");
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
                    exit(-7);
                }
            }

            if (c.hasError() && !cmd.hasOption("run-all")) {
                System.out.println("Error parsing file.");
                System.out.println(c.errorReport());
                continue SourceLoop;
                // System.exit(-8);
            }
        }
    }
}
