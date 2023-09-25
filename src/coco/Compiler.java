package coco;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import ast.AST;
import ast.DeclarationList;
import ast.RootAST;

public class Compiler {

    // Error Reporting ============================================================
    private StringBuilder errorBuffer = new StringBuilder();

    private String reportSyntaxError (NonTerminal nt) {
        String message = "SyntaxError(" + lineNumber() + "," + charPosition() + ")[Expected a token from " + nt.name() + " but got " + currentToken.kind + ".]";
        errorBuffer.append(message + "\n");
        return message;
    }

    private String reportSyntaxError (Token.Kind kind) {
        String message = "SyntaxError(" + lineNumber() + "," + charPosition() + ")[Expected " + kind + " but got " + currentToken.kind + ".]";
        errorBuffer.append(message + "\n");
        return message;
    }

    public String errorReport () {
        return errorBuffer.toString();
    }

    public boolean hasError () {
        return errorBuffer.length() != 0;
    }

    private class QuitParseException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public QuitParseException (String errorMessage) {
            super(errorMessage);
        }
    }

    private int lineNumber () {
        return currentToken.lineNumber();
    }

    private int charPosition () {
        return currentToken.charPosition();
    }

    // Compiler ===================================================================
    private Scanner scanner;
    private Token currentToken;

    private RootAST ast;
    private int numDataRegisters; // available registers are [1..numDataRegisters]
    private List<Integer> instructions;

    // Need to map from IDENT to memory offset

    public Compiler (Scanner scanner, int numRegs) {
        this.scanner = scanner;
        currentToken = this.scanner.next();
        numDataRegisters = numRegs;
        instructions = new ArrayList<>();
        ast = null;
    }

    //TODO
    public AST genAST() {
        computation();
        return ast;
    }
    
    public int[] compile () {
        initSymbolTable();
        try {
            computation();
            return instructions.stream().mapToInt(Integer::intValue).toArray();
        }
        catch (QuitParseException q) {
            errorBuffer.append("SyntaxError(" + lineNumber() + "," + charPosition() + ")");
            errorBuffer.append("[Could not complete parsing.]");
            return new ArrayList<Integer>().stream().mapToInt(Integer::intValue).toArray();
        }
    }

    // SymbolTable Management =====================================================
    private SymbolTable symbolTable;

    private void initSymbolTable () {
        throw new RuntimeException("implement initSymbolTable");
    }

    private void enterScope () {
        throw new RuntimeException("implement enterScope");
    }

    private void exitScope () {
        throw new RuntimeException("implement exitScope");
    }

    private Symbol tryResolveVariable (Token ident) {
        //TODO: Try resolving variable, handle SymbolNotFoundError
        return null;
    }

    private Symbol tryDeclareVariable (Token ident) {
        //TODO: Try declaring variable, handle RedeclarationError
        return null;
    }

    private String reportResolveSymbolError (String name, int lineNum, int charPos) {
        String message = "ResolveSymbolError(" + lineNum + "," + charPos + ")[Could not find " + name + ".]";
        errorBuffer.append(message + "\n");
        return message;
    }

    private String reportDeclareSymbolError (String name, int lineNum, int charPos) {
        String message = "DeclareSymbolError(" + lineNum + "," + charPos + ")[" + name + " already exists.]";
        errorBuffer.append(message + "\n");
        return message;
    }


    // Helper Methods =============================================================
    private boolean have (Token.Kind kind) {
        return currentToken.isKind(kind);
    }

    private boolean have (NonTerminal nt) {
        return nt.firstSet().contains(currentToken.kind);
    }

    private boolean accept (Token.Kind kind) {
        if (have(kind)) {
            try {
                currentToken = scanner.next();
            }
            catch (NoSuchElementException e) {
                if (!kind.equals(Token.Kind.EOF)) {
                    String errorMessage = reportSyntaxError(kind);
                    throw new QuitParseException(errorMessage);
                }
            }
            return true;
        }
        return false;
    }

    private boolean accept (NonTerminal nt) {
        if (have(nt)) {
            currentToken = scanner.next();
            return true;
        }
        return false;
    }

    private boolean expect (Token.Kind kind) {
        if (accept(kind)) {
            return true;
        }
        String errorMessage = reportSyntaxError(kind);
        throw new QuitParseException(errorMessage);
    }

    private boolean expect (NonTerminal nt) {
        if (accept(nt)) {
            return true;
        }
        String errorMessage = reportSyntaxError(nt);
        throw new QuitParseException(errorMessage);
    }

    private Token expectRetrieve (Token.Kind kind) {
        Token tok = currentToken;
        if (accept(kind)) {
            return tok;
        }
        String errorMessage = reportSyntaxError(kind);
        throw new QuitParseException(errorMessage);
    }

    private Token expectRetrieve (NonTerminal nt) {
        Token tok = currentToken;
        if (accept(nt)) {
            return tok;
        }
        String errorMessage = reportSyntaxError(nt);
        throw new QuitParseException(errorMessage);
    }


// Grammar Rules ==============================================================

    // function for matching rule that only expects nonterminal's FIRST set
    private Token matchNonTerminal (NonTerminal nt) {
        return expectRetrieve(nt);
    }

    // TODO: copy operators and type grammar rules from Compiler

    // literal = integerLit | floatLit
    private Token literal () {
        return matchNonTerminal(NonTerminal.LITERAL);
    }

    // TODO: copy remaining grammar rules from Compiler and make edits to build ast

    private ArrayType typeDecl() {
        Token type = expectRetrieve( NonTerminal.TYPE_DECL );

        ArrayList< Integer > dimensions = null;
        while( accept( Token.Kind.OPEN_BRACKET ) ) {

            Token dim = expectRetrieve(Token.Kind.INT_VAL);
            expect(Token.Kind.CLOSE_BRACKET);

            if( dimensions == null ) { dimensions = new ArrayList<>(); }
            dimensions.add( Integer.parseInt(dim.lexeme()) );

        }

        return new ArrayType(type, dimensions);
    }


    private ArrayList< Symbol > varDecl () {
        ArrayType arrtype = typeDecl(); // Array Identifier Generator
        Token.Kind type = arrtype.getType(); // Array Data Type
        Token ident = expectRetrieve( Token.Kind.IDENT ); // Initial Identifier

        // Check if a symbol exists with the given identifier.
        if( symbols.containsKey( ident.lexeme() ) ) {
            reportSyntaxError( ident.kind() );
            throw new Interpreter.QuitParseException("Duplicate identifier declaration: \"%s\" on line %d, char %d".formatted(ident.lexeme(), ident.lineNumber(), ident.charPosition()));
        }
        else {
            symbols.put( ident.lexeme(), arrtype );

            // Generate list of all identifiers for array. Returns single identifier for non-array type
            ArrayList< String > idents = arrtype.allIdents( ident );
            for( String name : idents ) {
                variables.put( name, new Variable( type ) ); // Create the appropriate variables
            }

        }

        while( accept(Token.Kind.COMMA ) ) {
            ident = expectRetrieve( Token.Kind.IDENT );

            // Check if a symbol exists with the given identifier.
            if( symbols.containsKey( ident.lexeme() ) ) {
                reportSyntaxError( ident.kind() );
                throw new Interpreter.QuitParseException("Duplicate identifier declaration: \"%s\" on line %d, char %d".formatted(ident.lexeme(), ident.lineNumber(), ident.charPosition()));
            }
            else {
                symbols.put( ident.lexeme(), arrtype );

                // Generate list of all identifiers for array. Returns single identifier for non-array type
                ArrayList< String > idents = arrtype.allIdents( ident );
                for( String name : idents ) {
                    variables.put( name, new Variable( type ) ); // Create the appropriate variables
                }

            }
        }

        expect( Token.Kind.SEMICOLON );
    }

    // computation	= "main" {varDecl} {funcDecl} "{" statSeq "}" "."
    private RootAST computation () {
        // throw new RuntimeException("implement all grammar rules to build ast");
        Token main = expectRetrieve(Token.Kind.MAIN);
        this.ast = new RootAST(main);

        if( have( NonTerminal.VAR_DECL ) ) {
            DeclarationList list = new DeclarationList(currentToken);

            while (have(NonTerminal.VAR_DECL)) {

            }
        }

        return ast;
    }
}
