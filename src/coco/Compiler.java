package coco;


import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import ast.*;

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
    private SymbolTable currentSymbolTable;

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
        initSymbolTable();
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

    private void initSymbolTable () {

        currentSymbolTable = new SymbolTable(null);

        // Add Default Function Declarations
        ArrayType readInt = ArrayType.makeFunctionType(
                new ArrayType( Token.Kind.INT ),
                new ArrayList<>()
        );
        currentSymbolTable.insert("readInt", new Symbol("readInt", readInt));

        ArrayType readFloat = ArrayType.makeFunctionType(
                Token.Kind.FLOAT
        );
        currentSymbolTable.insert("readFloat", new Symbol("readFloat", readFloat));

        ArrayType readBool = ArrayType.makeFunctionType(
                Token.Kind.BOOL
        );
        currentSymbolTable.insert("readBool", new Symbol("readBool", readBool));

        ArrayType printInt = ArrayType.makeFunctionType(
                Token.Kind.VOID,
                new Token.Kind[]{Token.Kind.INT}
        );
        currentSymbolTable.insert("printInt", new Symbol("printInt", printInt));

        ArrayType printFloat = ArrayType.makeFunctionType(
                Token.Kind.VOID,
                new Token.Kind[]{Token.Kind.FLOAT}
        );
        currentSymbolTable.insert("printFloat", new Symbol("printFloat", printFloat));

        ArrayType printBool = ArrayType.makeFunctionType(
                Token.Kind.VOID,
                new Token.Kind[]{Token.Kind.BOOL}
        );
        currentSymbolTable.insert("printBool", new Symbol("printBool", printBool));

        ArrayType printLn = ArrayType.makeFunctionType(
                Token.Kind.VOID
        );
        currentSymbolTable.insert("println", new Symbol("println", printLn));

    }

    private void enterScope () {
        currentSymbolTable = currentSymbolTable.pushScope();
    }

    private void exitScope () {
        currentSymbolTable = currentSymbolTable.popScope();
    }

    private Symbol tryResolveVariable (Token ident) {
        try{
            return currentSymbolTable.lookup(ident.lexeme());
        }catch(SymbolNotFoundError e){
            reportResolveSymbolError(ident.lexeme(), lineNumber(), charPosition());
        }
        //TODO: Try resolving variable, handle SymbolNotFoundError
        return null;
    }

    private Symbol tryDeclareVariable (Token ident) {
        try{
            return currentSymbolTable.insert(ident, null);
        }
        catch(RedeclarationError e){
            reportDeclareSymbolError(ident.lexeme(), lineNumber(), charPosition());
        }
        //TODO: Try declaring variable, handle RedeclarationError
        return null;
    }

    private Symbol tryDeclareVariable (Token ident, Symbol var) {
        try{
            return currentSymbolTable.insert(ident, var);
        }
        catch(RedeclarationError e){
            reportDeclareSymbolError(ident.lexeme(), lineNumber(), charPosition());
        }
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


    private ArrayList<VariableDeclaration> varDecl () {
        ArrayType arrtype = typeDecl(); // Array Identifier Generator
        Token.Kind type = arrtype.getType(); // Array Data Type
        Token ident = expectRetrieve( Token.Kind.IDENT ); // Initial Identifier

        ArrayList<VariableDeclaration> vars = new ArrayList<>();
        vars.add( new VariableDeclaration(ident, new Symbol(ident.lexeme(), arrtype) ) );

        while( accept(Token.Kind.COMMA ) ) {
            ident = expectRetrieve( Token.Kind.IDENT );
            vars.add( new VariableDeclaration(ident, new Symbol(ident.lexeme(), arrtype) ) );
        }

        expect( Token.Kind.SEMICOLON );

        return vars;
    }

    private FuncCall funcCall() {
        Token call = expectRetrieve( Token.Kind.CALL );
        Token func = expectRetrieve( Token.Kind.IDENT );
        expect( Token.Kind.OPEN_PAREN );

        Symbol sym = tryResolveVariable(func);

        ArrayList< AST > args = new ArrayList<>();
        FuncCall function = new FuncCall(call, sym);

        if( accept( Token.Kind.CLOSE_PAREN ) ) {
            return function;
        }
        else {

            ArrayList< AST > arguments = new ArrayList<>();
            ArgList list = new ArgList(currentToken);
            function.setArgs(list);
            if( have(NonTerminal.LITERAL ) ) {
                Token lit = expectRetrieve(NonTerminal.LITERAL);
                list.add( new IntegerLiteral(lit) );
            }
            expect( Token.Kind.CLOSE_PAREN );
            // arguments.add( relExpr( execute && !earlyReturn ) );
            // while( accept( Token.Kind.COMMA ) ) {
            //     arguments.add( relExpr( execute && !earlyReturn ) );
            // }
            // expect( Token.Kind.CLOSE_PAREN );


        }

        // Reached if not executing
        return function;
    }

    // computation	= "main" {varDecl} {funcDecl} "{" statSeq "}" "."
    private RootAST computation () {
        // throw new RuntimeException("implement all grammar rules to build ast");
        Token main = expectRetrieve(Token.Kind.MAIN);
        this.ast = new RootAST(main);

        if( have( NonTerminal.VAR_DECL ) ) {
            DeclarationList list = new DeclarationList(currentToken);

            while (have(NonTerminal.VAR_DECL)) {
                ArrayList<VariableDeclaration> decls = varDecl();
                for( VariableDeclaration decl : decls ) {
                    list.add( decl );
                }
            }

            ast.add( list );
        }

        expect(Token.Kind.OPEN_BRACE);

        while( have(NonTerminal.FUNC_CALL ) ) {
            ast.add( funcCall() );
            expect( Token.Kind.SEMICOLON );
        }

        expect(Token.Kind.CLOSE_BRACE);
        expect(Token.Kind.PERIOD);

        return ast;
    }
}
