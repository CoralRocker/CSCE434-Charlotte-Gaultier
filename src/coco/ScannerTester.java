package coco;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

// IMPORTANT: You need to put jar files in lib/ in your classpath: at the minimum commons-cli-1.5.0.jar
import org.apache.commons.cli.*;


public class ScannerTester {

    public static void main (String[] args) {
        Options options = new Options();
        options.addRequiredOption("s", "src", true, "Source File");
        options.addOption("c", "check", false, "Check against corresponding *.out file");


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
        boolean validate = cmd.hasOption("check");

        BufferedReader valid_output = null;

        if( validate ) {
            String vname = sourceFile.replace(".txt", ".out");
            try {
                valid_output = new BufferedReader( new FileReader( vname ));
            } catch (FileNotFoundException e) {
                validate = false;
            }
        }

        try {
            s = new Scanner(new FileReader(sourceFile));
        }
        catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error accessing the code file: \"" + sourceFile + "\"");
            System.exit(-2);
        }

        Token t;
        if( !s.hasNext() ) {
            System.out.println("Scanner Returned No Output!");
        }

        while (s.hasNext()) {
            t = s.next();
            StringBuilder outstr = new StringBuilder();
            outstr.append( t.kind.toString() );
            // System.out.print(t.kind);
            switch (t.kind) {
                case INT_VAL:
                case FLOAT_VAL:
                case IDENT:
                    outstr.append("\t");
                    outstr.append(t.lexeme());
                    // System.out.println("\t" + t.lexeme());
                    break;
                default:
                    // System.out.println();
                    break;
            }

            if( validate ) {
                String line = null;
                String vstr = outstr.toString();
                try {
                    line = valid_output.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if( line == null ) {
                    System.err.printf("Validation File Ended, but Scanner still outputting!\n");
                }
                else if( !Objects.equals(line, vstr) ) {
                    System.err.printf("Valid: \"%s\" != Scanner \"%s\"\n", line, vstr );
                }
            }

            System.out.println( outstr.toString() );
        }
    }
}
