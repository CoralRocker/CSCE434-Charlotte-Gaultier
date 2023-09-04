package coco;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scanner implements Iterator<Token> {

    private BufferedReader input;   // buffered reader to read file
    private boolean closed; // flag for whether reader is closed or not

    private int lineNum;    // current line number
    private int charPos;    // character offset on current line

    private String scan = null;    // current lexeme being scanned in
    private int nextChar;   // contains the next char (-1 == EOF)

    // reader will be a FileReader over the source file
    public Scanner (Reader reader) throws IOException {
        input = new BufferedReader( reader );
        closed = false;
    }

    // signal an error message
    public void Error (String msg, Exception e) {
        System.err.println("Scanner: Line - " + lineNum + ", Char - " + charPos);
        if (e != null) {
            e.printStackTrace();
        }
        System.err.println(msg);
    }

    private String nextLine() {
        String line;

        try {
            line = input.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return line;
    }

    /*
     * helper function for reading a single char from input
     * can be used to catch and handle any IOExceptions,
     * advance the charPos or lineNum, etc.
     */
    private int readChar () {
        int c;
        try {
            c = input.read();
        } catch (IOException e) {
            Error("IOException in reading file", e);
            throw new RuntimeException(e);
        }

        if( c == -1 ) {
            return -1;
        }
        else if( c == 10 ) {
            charPos = -1;
            lineNum++;
        }
        else {
            charPos++;
        }

        return c;
    }

    private String scanSubstr(int length) {
        if( (charPos + length) >= scan.length() ) {
            return null;
        }

        return scan.substring(charPos, charPos+length);
    }

    private int toklen( Token token ) {
        if( token == null ) {
            return 0;
        }

        return token.length();
    }

    /*
     * function to query whether or not more characters can be read
     * depends on closed and nextChar
     */
    @Override
    public boolean hasNext () {
        return !closed && nextChar != -1;
    }

    private void stripLeadingWhitespace() {
        // Skip All Whitespace
        while( charPos < scan.length() && Character.isWhitespace( scan.charAt( charPos ) ) ) {
            charPos++;
        }
    }

    private Token updateLine() {
        scan = nextLine();
        if( scan == null ) {
            try {
                input.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            closed = true;
            return Token.EOF( lineNum, charPos );
        }
        charPos = 0;
        lineNum++;

        return null;
    }

    /*
     *	returns next Token from input
     *
     *  invariants:
     *  1. call assumes that nextChar is already holding an unread character
     *  2. return leaves nextChar containing an untokenized character
     *  3. closes reader when emitting EOF
     */
    @Override
    public Token next () {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        // If a line has not been read in, attempt to. Return EOF if the input is over.
        if( scan == null ) {
            Token tkn = updateLine();
            // EOF condition
            if( tkn != null ) {
                return tkn;
            }
        }

        // Strip all whitespace, traversing newlines, until something scannable is found.
        // May return EOF when encountered.
        stripLeadingWhitespace();
        while( charPos >= scan.length() ) {
            Token tkn = updateLine();
            // EOF condition
            if (tkn != null) {
                return tkn;
            }
            if (scan.isEmpty()) {
                continue;
            }
            stripLeadingWhitespace();
        }

        // ArrayList of extracted tokens.
        ArrayList< Token > tokens = new ArrayList<>();

        String scannable = scan.substring(charPos);

        Token symOp = extractOperator();
        tokens.add(symOp);

        // Check if operator is a comment type.
        if( symOp != null ) {
            if ( symOp.kind == Token.Kind.LINE_COMMENT ) {
                // System.out.printf("Line %d(%d) is commented: %s\n", lineNum, charPos, scan);
                charPos = scan.length();
                return next();
            }
            else if ( symOp.kind ==  Token.Kind.START_BLOCK_COMMENT  ) {
                Token endBlock = findEndBlockComment();
                if( endBlock.isKind( Token.Kind.EOF ) ) {
                    return Token.ERROR("Missing end of block comment", lineNum, charPos );
                }

                return next();
            }
        }

        tokens.add( extractIdentOrKeyword() );

        tokens.add( extractInteger() );

        tokens.add( extractFloat() );

        // Find the longest token found (Maximal Munchies)
        int argmax = -1;
        int maxval = 0;
        for( int i = 0; i < tokens.size(); i++ ) {
            if( toklen(tokens.get(i)) > maxval ) {
                argmax = i;
                maxval = toklen( tokens.get(i) );
            }
        }

        // In case of no munchability, return an error
        if( argmax == -1 ) {
            System.out.printf("Input \"%s\" has no parseable input.\n", scan.substring(charPos));
            return null;
        }
        else {
            Token token = tokens.get(argmax);
            charPos += token.length();
            return token;
        }
    }


    /**
     * @brief Attempt to extract an operator from the scan.
     * @return The operator token, or null if none found.
     */
    private Token extractOperator() {
        Pattern rxp = Pattern.compile("^([\\Q%^,!<>(){}[]=+-.*/;:\\E]){1,2}");
        Matcher matches = rxp.matcher(scan.substring(charPos));

        if( matches.find() ) {
            String tkn = matches.group();

            Token found = null;
            for(Token.Kind kind : Token.Kind.values()) {
                if( Objects.equals(tkn, kind.getDefaultLexeme()) ) {
                    found = new Token( kind, lineNum, charPos );
                }
            }
            if( found == null ) {
                tkn = tkn.substring(0, 1);
                for(Token.Kind kind : Token.Kind.values()) {
                    if( Objects.equals(tkn, kind.getDefaultLexeme()) ) {
                        found = new Token( kind, lineNum, charPos );
                    }
                }
            }

            if( found != null ) {
                return found;
            }
        }

        return null;
    }

    /**
     * @brief Attempt to extract either a keyword or an identifier from the scan.
     *
     * @return The identifier or keyword token, or null if none are found.
     */
    private Token extractIdentOrKeyword() {

        Pattern rxp = Pattern.compile("^([a-zA-Z][a-zA-Z0-9_]*)");
        Matcher match = rxp.matcher(scan.substring(charPos));
        if( ! match.find() ) {
            return null;
        }

        String tkn = match.group();

        Token matched = null;

        for( Token.Kind kind : Token.Kind.values() ) {
            if( Objects.equals(kind.getDefaultLexeme(), tkn) ) {
                matched = new Token( kind, lineNum, charPos );
            }
        }

        if( matched == null ){
            matched = Token.IDENT(tkn, lineNum, charPos);
        }

        return matched;
    }

    /**
     * @brief Attempt to extract an integer value. Returns null if none found.
     * @return The found integer token or null.
     */
    private Token extractInteger() {
       Pattern rxp = Pattern.compile("^-?\\d+");
       Matcher matches = rxp.matcher(scan.substring(charPos));

       if( !matches.find() ) {
           return null;
       }

       // System.out.printf("Found Integer: %s in \"%s\"\n", matches.group(), scan.substring(charPos));

       return Token.INT_VAL( matches.group(), lineNum, charPos );
    }

    /**
     * @brief Attempt to extract a float value. Return an error if the value ends with a decimal point.
     *
     * @return The float value token or an error token or null.
     */
    private Token extractFloat() {
        Pattern rxp = Pattern.compile("^-?\\d+\\.\\d*");
        Matcher matches = rxp.matcher(scan.substring(charPos));

        if( !matches.find() ) {
            return null;
        }

        String lexeme = matches.group();

        // System.out.printf("Found Float: %s in \"%s\"\n", lexeme, scan.substring(charPos));

        if( lexeme.endsWith(".") ) {
            return Token.ERROR("Float may not end in decimal point", lineNum, charPos);
        }

        return Token.FLOAT_VAL( lexeme, lineNum, charPos );
    }

    /**
     * @brief Find the next occurence of the "end block comment" operator ( * / ). Return EOF if not found, but do not process EOF.
     *
     * @return The end block comment token or an EOF. Never returns null.
     */
    private Token findEndBlockComment() {

        Pattern rxp = Pattern.compile("\\*/");
        Matcher matches = rxp.matcher(scan.substring(charPos));

        while( ! matches.find() ) {
            scan = nextLine();
            if( scan == null ) {
                return Token.EOF(lineNum, charPos);
            }
            charPos = 0;
            lineNum++;
            matches = rxp.matcher(scan);
        }

        // System.out.printf("End Comment: %d :: %d: \"%s\"\n", lineNum, matches.end(), scan );
        charPos += matches.end();

        return new Token( Token.Kind.END_BLOCK_COMMENT, lineNum, matches.end()-2 );
    }
}
