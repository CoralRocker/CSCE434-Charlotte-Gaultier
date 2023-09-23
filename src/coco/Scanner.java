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

    private BacktrackableLineReader input;   // buffered reader to read file
    private boolean closed; // flag for whether reader is closed or not

    private int lineNum;    // current line number
    private int charPos;    // character offset on current line

    private String scan = null;    // current lexeme being scanned in
    private int nextChar;   // contains the next char (-1 == EOF)

    // reader will be a FileReader over the source file
    public Scanner (Reader reader) throws IOException {
        input = new BacktrackableLineReader( reader );
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
        while( charPos <= scan.length() && Character.isWhitespace( scan.charAt( charPos - 1 ) ) ) {
            charPos++;
        }
    }

    private String scanString() {
        return scan.substring(charPos-1);
    }

    /**
     * @brief Get the next line from the input. Update line number and char pos. Return EOF if condition met.
     * @return Null if line is OK, else EOF token.
     */
    private Token updateLine(boolean close) {
        scan = nextLine();
        if( scan == null ) {
            if( close ) {
                try {
                    input.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                closed = true;
            }
            return Token.EOF( lineNum, charPos );
        }
        charPos = 1;
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
            Token tkn = updateLine(true);
            // EOF condition
            if( tkn != null ) {
                return tkn;
            }
        }

        // Strip all whitespace, traversing newlines, until something scannable is found.
        // May return EOF when encountered.
        stripLeadingWhitespace();
        while( charPos > scan.length() ) {
            Token tkn = updateLine(true);
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

        String scannable = scanString();

        Token symOp = extractOperator();
        tokens.add(symOp);

        // Check if operator is a comment type.
        if( symOp != null ) {
            switch( symOp.kind ) {
                case LINE_COMMENT -> {
                    charPos += scan.length();
                    return next();
                }
                case START_BLOCK_COMMENT -> {
                    charPos += symOp.length();
                    Token endBlock = findEndBlockComment();
                    if( endBlock.isKind( Token.Kind.EOF ) ) {
                        return Token.ERROR("Missing end of block comment", lineNum, charPos );
                    }

                    return next();
                }
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
            // System.out.printf("Input \"%s\" has no parseable input.\n", scannable);
            int pos = scannable.length();
            for( int i = 0; i < scannable.length(); i++ ) {
                if( Character.isWhitespace(scannable.charAt(i)) ) {
                    pos = i;
                    break;
                }
            }
            scannable = scannable.substring(0, pos);
            charPos += scannable.length();

            Token err = Token.ERROR(scannable, lineNum, charPos);
            // System.out.printf("Error: %s\n", err);
            return err;
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
        Matcher matches = rxp.matcher(scanString());

        if( matches.find() ) {
            String len2 = matches.group();
            String len1 = len2.substring(0, 1);

            Token found2 = null;
            Token found1 = null;

            for(Token.Kind kind : Token.operators) {
                if( Objects.equals(len2, kind.getDefaultLexeme()) ) {
                    found2 = new Token( kind, lineNum, charPos );
                }
                if( Objects.equals(len1, kind.getDefaultLexeme()) ) {
                    found1 = new Token( kind, lineNum, charPos );
                }
            }

            if( found2 != null ) {
                return found2;
            }
            else if( found1 != null ) {
                return found1;
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
        Matcher match = rxp.matcher(scanString());
        if( ! match.find() ) {
            return null;
        }

        String tkn = match.group();

        Token matched = null;

        for( Token.Kind kind : Token.keywords ) {
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
       Matcher matches = rxp.matcher(scanString());

       if( !matches.find() ) {
           return null;
       }

       return Token.INT_VAL( matches.group(), lineNum, charPos );
    }

    /**
     * @brief Attempt to extract a float value. Return an error if the value ends with a decimal point.
     *
     * @return The float value token or an error token or null.
     */
    private Token extractFloat() {
        Pattern rxp = Pattern.compile("^-?\\d+\\.\\d*");
        Matcher matches = rxp.matcher(scanString());

        if( !matches.find() ) {
            return null;
        }

        String lexeme = matches.group();

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
        Matcher matches = rxp.matcher(scanString());

        while( ! matches.find() ) {
            Token tkn = updateLine(false);
            if( tkn != null ) {
                return tkn;
            }
            matches = rxp.matcher(scan);
        }

        charPos += matches.end();

        return new Token( Token.Kind.END_BLOCK_COMMENT, lineNum, matches.end()-2 );
    }

    public void backtrack( int line ) {
        try {
            input.backtrack(line);
            scan = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setPos( int line, int pos ) {
        try {
            input.backtrack(line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        updateLine(false);
        lineNum = line;
        charPos = pos;
    }

    // Backtrack to right after given token
    public void backtrack( Token tkn ) {
        setPos( tkn.lineNumber(), tkn.charPosition() );
    }
}
