package coco;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Token {

    public enum Kind {
        // boolean operators
        AND("and"),
        OR("or"),
        NOT("not"),

        // arithmetic operators
        POW("^"),

        MUL("*"),
        DIV("/"),
        MOD("%"),

        ADD("+"),
        SUB("-"),

        // relational operators
        EQUAL_TO("=="),
        NOT_EQUAL("!="),
        LESS_THAN("<"),
        LESS_EQUAL("<="),
        GREATER_EQUAL(">="),
        GREATER_THAN(">"),

        // assignment operators
        ASSIGN("="),
        ADD_ASSIGN("+="),
        SUB_ASSIGN("-="),
        MUL_ASSIGN("*="),
        DIV_ASSIGN("/="),
        MOD_ASSIGN("%="),
        POW_ASSIGN("^="),

        // unary increment/decrement
        UNI_INC("++"),
        UNI_DEC("--"),

        // primitive types
        VOID("void"),
        BOOL("bool"),
        INT("int"),
        FLOAT("float"),

        // boolean literals
        TRUE("true"),
        FALSE("false"),

        // region delimiters
        OPEN_PAREN("("),
        CLOSE_PAREN(")"),
        OPEN_BRACE("{"),
        CLOSE_BRACE("}"),
        OPEN_BRACKET("["),
        CLOSE_BRACKET("]"),

        // field/record delimiters
        COMMA(","),
        COLON(":"),
        SEMICOLON(";"),
        PERIOD("."),

        // control flow statements
        IF("if"),
        THEN("then"),
        ELSE("else"),
        FI("fi"),

        WHILE("while"),
        DO("do"),
        OD("od"),

        REPEAT("repeat"),
        UNTIL("until"),

        CALL("call"),
        RETURN("return"),

        // keywords
        MAIN("main"),
        FUNC("function"),

        // special cases
        INT_VAL(),
        FLOAT_VAL(),
        IDENT(),

        EOF(),

        LINE_COMMENT("//"),
        START_BLOCK_COMMENT("/*"),
        END_BLOCK_COMMENT("*/"),

        ERROR();

        private String defaultLexeme;

        Kind () {
            defaultLexeme = "";
        }

        Kind (String lexeme) {
            defaultLexeme = lexeme;
        }

        public boolean hasStaticLexeme () {
            return defaultLexeme != null;
        }

        public String getDefaultLexeme() { return defaultLexeme; }

        // OPTIONAL: convenience function - boolean matches (String lexeme)
        //           to report whether a Token.Kind has the given lexeme
        //           may be useful
    }

    public static HashSet< Kind > operators = new HashSet<>() {
        private static final long serialVersionUID = 1L;

        {
            add(Kind.START_BLOCK_COMMENT);
            add(Kind.LINE_COMMENT);
            add(Kind.POW);
            add(Kind.MUL);
            add(Kind.DIV);
            add(Kind.MOD);
            add(Kind.ADD);
            add(Kind.SUB);
            add(Kind.EQUAL_TO);
            add(Kind.NOT_EQUAL);
            add(Kind.LESS_THAN);
            add(Kind.LESS_EQUAL);
            add(Kind.GREATER_EQUAL);
            add(Kind.GREATER_THAN);
            add(Kind.ASSIGN);
            add(Kind.ADD_ASSIGN);
            add(Kind.SUB_ASSIGN);
            add(Kind.MUL_ASSIGN);
            add(Kind.DIV_ASSIGN);
            add(Kind.MOD_ASSIGN);
            add(Kind.POW_ASSIGN);
            add(Kind.UNI_INC);
            add(Kind.UNI_DEC);
            add(Kind.OPEN_PAREN);
            add(Kind.CLOSE_PAREN);
            add(Kind.OPEN_BRACE);
            add(Kind.CLOSE_BRACE);
            add(Kind.OPEN_BRACKET);
            add(Kind.CLOSE_BRACKET);
            add(Kind.COMMA);
            add(Kind.COLON);
            add(Kind.SEMICOLON);
            add(Kind.PERIOD);
        }
    };

    public static HashSet< Kind > keywords = new HashSet<>() {
        private static final long serialVersionUID = 1L;

        {
            add( Kind.IF );
            add( Kind.THEN );
            add( Kind.ELSE );
            add( Kind.FI );
            add( Kind.WHILE );
            add( Kind.DO );
            add( Kind.OD );
            add( Kind.REPEAT );
            add( Kind.UNTIL );
            add( Kind.CALL );
            add( Kind.RETURN );
            add( Kind.MAIN );
            add( Kind.FUNC );
            add( Kind.VOID );
            add( Kind.BOOL );
            add( Kind.INT );
            add( Kind.FLOAT );
            add( Kind.TRUE );
            add( Kind.FALSE );
            add( Token.Kind.AND );
            add( Token.Kind.OR );
            add( Token.Kind.NOT );
        }
    };
    private int lineNum;
    private int charPos;
    Kind kind;  // package-private
    private String lexeme = "";


    // TODO: implement remaining factory functions for handling special cases (EOF below)

    public static Token EOF (int linePos, int charPos) {
        Token tok = new Token(linePos, charPos);
        tok.kind = Kind.EOF;
        return tok;
    }

    public static Token IDENT(String lexeme, int linePos, int charPos ) {
        Token token = new Token( linePos, charPos );
        token.kind = Kind.IDENT;
        token.lexeme = lexeme;
        return token;
    }

    public static Token LINE_COMMENT( int linePos, int charPos ) {
        Token token = new Token(linePos, charPos);
        token.kind = Kind.LINE_COMMENT;
        token.lexeme = "";
        return token;
    }

    public static Token ERROR( String err, int linePos, int charPos ) {
        Token token = new Token( linePos, charPos );
        token.kind = Kind.ERROR;
        token.lexeme = err;
        return token;
    }

    public static Token INT_VAL( String lexeme, int linePos, int charPos ) {
        Token token = new Token( linePos, charPos );
        token.kind = Kind.INT_VAL;
        token.lexeme = lexeme;
        return token;
    }

    public static Token FLOAT_VAL( String lexeme, int linePos, int charPos ) {
        Token token = new Token( linePos, charPos );
        token.kind = Kind.FLOAT_VAL;
        token.lexeme = lexeme;
        return token;
    }

    private Token (int lineNum, int charPos) {
        this.lineNum = lineNum;
        this.charPos = charPos;

        // no lexeme provide, signal error
        this.kind = Kind.ERROR;
        this.lexeme = "No Lexeme Given";
    }

    public Token (String lexeme, int lineNum, int charPos) {
        this.lineNum = lineNum;
        this.charPos = charPos;

        // TODO: based on the given lexeme determine and set the actual kind

        // if we don't match anything, signal error
        this.kind = Kind.ERROR;
        this.lexeme = "Unrecognized lexeme: " + lexeme;
    }

    public Token( Kind pKind, int lineNum, int charPos ) {
        this.lineNum = lineNum;
        this.charPos = charPos;

        this.kind = pKind;
        this.lexeme = pKind.defaultLexeme;
    }

    public int lineNumber () {
        return lineNum;
    }

    public int charPosition () {
        return charPos;
    }

    public String lexeme () {
        return lexeme;
    }

    public Kind kind () {
        return kind;
    }

    // TODO: function to query a token about its kind - boolean is (Token.Kind kind)
    public boolean isKind( Kind pKind ) {
        return kind == pKind;
    }

    // OPTIONAL: add any additional helper or convenience methods
    //           that you find make for a cleaner design

    public int length() {
        return lexeme.length();
    }

    @Override
    public String toString () {
        return "Line: " + lineNum + ", Char: " + charPos + ", Lexeme: " + lexeme;
    }
}
