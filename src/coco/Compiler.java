package coco;


import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

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

    private FuncBody unresolvedFunction = null;
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

    public SymbolTable symbolTable(){
        return currentSymbolTable;
    }

    //TODO
    public AST genAST() {

        try {
            initSymbolTable();
            computation();
        }
        catch( QuitParseException e ) {
//            System.out.println("CAUGHT ERROR: " + e);
        }
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
        currentSymbolTable.insert("readInt", new FunctionSymbol("readInt", readInt));

        ArrayType readFloat = ArrayType.makeFunctionType(
                Token.Kind.FLOAT
        );
        currentSymbolTable.insert("readFloat", new FunctionSymbol("readFloat", readFloat));

        ArrayType readBool = ArrayType.makeFunctionType(
                Token.Kind.BOOL
        );
        currentSymbolTable.insert("readBool", new FunctionSymbol("readBool", readBool));

        ArrayType printInt = ArrayType.makeFunctionType(
                Token.Kind.VOID,
                new Token.Kind[]{Token.Kind.INT}
        );
        currentSymbolTable.insert("printInt", new FunctionSymbol("printInt", printInt));

        ArrayType printFloat = ArrayType.makeFunctionType(
                Token.Kind.VOID,
                new Token.Kind[]{Token.Kind.FLOAT}
        );
        currentSymbolTable.insert("printFloat", new FunctionSymbol("printFloat", printFloat));

        ArrayType printBool = ArrayType.makeFunctionType(
                Token.Kind.VOID,
                new Token.Kind[]{Token.Kind.BOOL}
        );
        currentSymbolTable.insert("printBool", new FunctionSymbol("printBool", printBool));

        ArrayType println = ArrayType.makeFunctionType(
                Token.Kind.VOID
        );
        currentSymbolTable.insert("println", new FunctionSymbol("println", println));

    }

    private void enterScope () {
        currentSymbolTable = currentSymbolTable.pushScope();
    }

    private void exitScope () {
        currentSymbolTable = currentSymbolTable.popScope();
    }

    private Symbol tryResolveVariable (Token ident) {
        try{
            return currentSymbolTable.lookup(ident);
        }catch(SymbolNotFoundError e){
            if( unresolvedFunction != null ) {
                unresolvedFunction.addUnresolved(ident);
            }
            else {
                reportResolveSymbolError(ident.lexeme(), ident.lineNumber(), ident.charPosition());
            }
        }
        //TODO: Try resolving variable, handle SymbolNotFoundError
        return new VariableSymbol("ERROR", new ArrayType(Token.Kind.ERROR));
    }
    private Symbol tryAssignVariable (Token ident, Symbol var) {
        try{
            return currentSymbolTable.assign(ident, var);
        }
        catch(SymbolNotFoundError e){
            reportResolveSymbolError(ident.lexeme(), ident.lineNumber(), ident.charPosition());
        }

        return new VariableSymbol("ERROR", new ArrayType(Token.Kind.ERROR));

    }

    private Symbol tryDeclareVariable (Token ident) {
        try{
            return currentSymbolTable.insert(ident, null);
        }
        catch(RedeclarationError e){
            reportDeclareSymbolError(ident.lexeme(), ident.lineNumber(), ident.charPosition());
        }
        //TODO: Try declaring variable, handle RedeclarationError
        return new VariableSymbol("ERROR", new ArrayType(Token.Kind.ERROR));

    }

    private Symbol tryDeclareVariable (Token ident, Symbol var) {
        try{
            return currentSymbolTable.insert(ident, var);
        }
        catch(RedeclarationError e){
            reportDeclareSymbolError(ident.lexeme(), ident.lineNumber(), ident.charPosition());
        }
        //TODO: Try declaring variable, handle RedeclarationError
        return new VariableSymbol("ERROR", new ArrayType(Token.Kind.ERROR));

    }

    private Symbol tryDeclareFunction(Token func, ArrayType type) {
        Symbol sym = null;
        if( currentSymbolTable.contains(func) ) {
            sym = currentSymbolTable.lookup(func);

            if( ! (sym instanceof FunctionSymbol) ) {
                throw new RuntimeException(String.format("Function %s is not a function identifier!", func.lexeme()));
            }
        }
        else {
            sym = currentSymbolTable.insert(func, new FunctionSymbol(func.lexeme()) );
        }

        FunctionSymbol funcSym = (FunctionSymbol) sym;
        funcSym.add(type);

        return funcSym;
    }

    private FunctionSymbol tryResolveFunction(Token func) {
        Symbol sym = null;
        if( !currentSymbolTable.contains(func) ) {
            reportResolveSymbolError(func.lexeme(), func.lineNumber(), func.endCharPos());
        }
        sym = currentSymbolTable.lookup(func);
        if( ! (sym instanceof FunctionSymbol) ) {
            throw new RuntimeException(String.format("Function call %s is not a function! (%s)", func, sym));
        }

        return (FunctionSymbol) sym;
    }

    private Symbol tryDeclareVariable (String str, Symbol var) {
        try{
            return currentSymbolTable.insert(str, var);
        }
        catch(RedeclarationError e){
            reportDeclareSymbolError(str, lineNumber(), charPosition());
        }
        //TODO: Try declaring variable, handle RedeclarationError
        return new VariableSymbol("ERROR", new ArrayType(Token.Kind.ERROR));

    }

    private void reportResolveSymbolError (String name, int lineNum, int charPos) {
        if( errorBuffer.isEmpty() ) {
//            errorBuffer.append("Error parsing file.\n");
        }
        String message = "ResolveSymbolError(" + lineNum + "," + charPos + ")[Could not find " + name + ".]";
        errorBuffer.append(message);
//        throw new QuitParseException(message);
    }

    private void reportDeclareSymbolError (String name, int lineNum, int charPos) {
        if( errorBuffer.isEmpty() ) {
        //    errorBuffer.append("Error parsing file.\n");
        }
        String message = "DeclareSymbolError(" + lineNum + "," + charPos + ")[" + name + " already exists.]";
        errorBuffer.append(message);
//        return
//        throw new QuitParseException(message);
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

    private AST designator ( ) {
        int lineNum = lineNumber();
        int charPos = charPosition();

        Token ident = expectRetrieve(Token.Kind.IDENT);
        AST symbol = new Designator(ident, tryResolveVariable(ident));

        ArrayList<AST> indexes = new ArrayList<>();
        while( accept( Token.Kind.OPEN_BRACKET ) ) {
            AST dim = relExpr();
            expect( Token.Kind.CLOSE_BRACKET );

            indexes.add( dim );
        }

        for( int i = indexes.size()-1; i >= 0; i-- ) {
            symbol = new ArrayIndex(indexes.get(i).token(), symbol, indexes.get(i));
        }

        return symbol;
    }


    private AST addExpr( ) {
        AST var = multExpr();

        while( have( NonTerminal.ADD_OP ) ) {
            Token op;
            AST rval;
            op = expectRetrieve(NonTerminal.ADD_OP);
            rval = multExpr();

            switch ( op.kind() ) {
                case ADD -> {
                    var = new Addition(op, var, rval);
                }
                case SUB -> {
                    var = new Subtraction(op, var, rval);
                }
                case OR -> {
                    var = new LogicalOr( op, var, rval );
                }
            }
        }

        return var;
    }

    private AST multExpr( ) {
        AST var = powExpr();

        while( have( NonTerminal.MUL_OP ) ) {
            Token op = expectRetrieve( NonTerminal.MUL_OP );
            AST rval = powExpr();

            switch( op.kind() ) {
                case MUL -> {
                    var = new Multiplication(op, var, rval);
                }
                case DIV -> {
                    var = new Division(op, var, rval);
                }
                case MOD -> {
                    var = new Modulo(op, var, rval);
                }
                case AND -> {
                    var = new LogicalAnd(op, var, rval);
                }
            }
        }

        return var;
    }

    private AST powExpr() {
        AST var = groupExpr();

        while( have( Token.Kind.POW ) ) {
            Token op = expectRetrieve(Token.Kind.POW);
            AST rval = groupExpr();

            var = new Power(op, var, rval);
        }

        return var;
    }

    private AST groupExpr() {
        if( have( NonTerminal.LITERAL ) ) {
            Token lit = expectRetrieve( NonTerminal.LITERAL );
            switch ( lit.kind ) {
                case FLOAT_VAL -> {
                    return new FloatLiteral( lit );
                }
                case INT_VAL -> {
                    return new IntegerLiteral( lit );
                }
                case TRUE, FALSE -> {
                    return new BoolLiteral( lit );
                }
            }
        }
        else if( have( Token.Kind.IDENT ) ) {
            return designator();
        }
        else if( have( Token.Kind.NOT ) ) {
            Token not = expectRetrieve(Token.Kind.NOT);
            AST var = relExpr();
            return new LogicalNot( not, var );
        }
        else if( have( Token.Kind.OPEN_PAREN ) ) {
            return relation();
        } else if (have(Token.Kind.CALL) ) {
            return funcCall();
        }
        else {
            String err = reportSyntaxError( NonTerminal.GROUP_EXPR );
            throw new Interpreter.QuitParseException(err);
        }

        return null;
    }

    private AST relExpr() {
        AST lval = addExpr();

        while( have( NonTerminal.REL_OP ) ) {
            Token op = expectRetrieve( NonTerminal.REL_OP );
            AST rval =  addExpr();

            lval = new Relation(op, lval, rval);
        }

        return lval;
    }

    private AST relation() {
        expect( Token.Kind.OPEN_PAREN );
        AST var = relExpr();
        expect( Token.Kind.CLOSE_PAREN );

        return var;
    }


    private ArrayList<VariableDeclaration> varDecl () {
        ArrayType arrtype = typeDecl(); // Array Identifier Generator
        Token.Kind type = arrtype.getType(); // Array Data Type
        Token ident = expectRetrieve( Token.Kind.IDENT ); // Initial Identifier

        ArrayList<VariableDeclaration> vars = new ArrayList<>();
        VariableDeclaration decl = new VariableDeclaration(ident, new VariableSymbol(ident.lexeme(), arrtype) );
        vars.add( decl );
        tryDeclareVariable(ident.lexeme(), decl.symbol());

        while( accept(Token.Kind.COMMA ) ) {
            ident = expectRetrieve( Token.Kind.IDENT );
            decl = new VariableDeclaration(ident, new VariableSymbol(ident.lexeme(), arrtype) );
            tryDeclareVariable(ident.lexeme(), decl.symbol());
            vars.add( decl );
        }

        expect( Token.Kind.SEMICOLON );

        return vars;
    }

    private FuncCall funcCall() {
        Token call = expectRetrieve( Token.Kind.CALL );
        Token func = expectRetrieve( Token.Kind.IDENT );
        expect( Token.Kind.OPEN_PAREN );

        FunctionSymbol sym = tryResolveFunction(func);

        ArrayList< AST > args = new ArrayList<>();
        FuncCall function = new FuncCall(call, sym);

        ArrayList< AST > arguments = new ArrayList<>();
        ArgList list = new ArgList(currentToken);
        function.setArgs(list);
        if( !have( Token.Kind.CLOSE_PAREN ) ) {
            list.add(relExpr());
            while ( accept(Token.Kind.COMMA)) {
                list.add(relExpr());
            }
        }
        expect( Token.Kind.CLOSE_PAREN );

        return function;
    }

    private AST assign() {
        AST var = designator();
        Token op;

        Assignment expr = null;

        if( have( NonTerminal.UNARY_OP ) ) {
            op = expectRetrieve( NonTerminal.UNARY_OP );

            AST opr = null;

            switch (op.lexeme()) {
                case "++" -> {
                    // var.set(var.getInt() + 1);
                    opr = new Addition(op, var, new IntegerLiteral(Token.INT_VAL("1", 0, 0)));
                }
                case "--" -> {
                    opr = new Subtraction(op, var, new IntegerLiteral(Token.INT_VAL("1", 0, 0)));
                }
                default -> {
                    String err = reportSyntaxError(NonTerminal.UNARY_OP);
                    throw new Interpreter.QuitParseException("Unable to parse unary op \"%s\": %s".formatted(op.lexeme(), err));
                }
            }

            expr = new Assignment( var.token(), var, opr);
        }
        else {
            op = expectRetrieve( NonTerminal.ASSIGN_OP );
            AST rvalue = relExpr();
            switch (op.lexeme()) {
                case "=" -> {
                    expr = new Assignment(var.token(), var, rvalue);
                }
                case "+=" -> {
                    expr = new Assignment(var.token(), var,
                        new Addition( op, var, rvalue )
                    );
                }
                case "-=" -> {
                    expr = new Assignment(var.token(), var,
                        new Subtraction( op, var, rvalue )
                    );
                }
                case "*=" -> {
                    expr = new Assignment(var.token(), var,
                        new Multiplication( op, var, rvalue )
                    );
                }
                case "%=" -> {
                    expr = new Assignment(var.token(), var,
                        new Modulo( op, var, rvalue )
                    );
                }
                case "/=" -> {
                    expr = new Assignment(var.token(), var,
                        new Division( op, var, rvalue)
                    );
                }
                case "^=" -> {
                    expr = new Assignment(var.token(), var,
                        new Power( op, var, rvalue )
                    );
                }
            }
        }

        return expr;
    }

    private AST ifStat( ) {
        Token tkn = expectRetrieve( Token.Kind.IF );
        Relation bool = (Relation) relation();
        expect( Token.Kind.THEN );
        StatSeq ifseq = statSeq();
        StatSeq elseseq = null;

        if( accept( Token.Kind.ELSE ) ) {
            elseseq = statSeq();
        }

        expect( Token.Kind.FI );

        return new IfStat( tkn, bool, ifseq, elseseq);
    }

    private AST returnStat() {
        Token ret = expectRetrieve( Token.Kind.RETURN );

        if( have( NonTerminal.GROUP_EXPR ) ) {
            AST retval = relExpr();
            return new Return(ret, retval);
        }
        else {
            return new Return(ret);
        }
    }

    private AST whileStat() {
        Token wtok = expectRetrieve( Token.Kind.WHILE );
        AST cond = relation();
        expectRetrieve( Token.Kind.DO );
        StatSeq seq = statSeq();

        expect( Token.Kind.OD );

        return new WhileStat(wtok, cond, seq);
    }

    private AST repeatStat() {
        Token rep = expectRetrieve( Token.Kind.REPEAT );
        StatSeq seq = statSeq();
        expect( Token.Kind.UNTIL );
        AST cond = relation();

        return new RepeatStat(rep, cond, seq);
    }

    private StatSeq statSeq() {
        if( ! have( NonTerminal.STATEMENT) ) {
            String err = reportSyntaxError(NonTerminal.STATEMENT);
            throw new Interpreter.QuitParseException(err);
        }

        StatSeq seq = new StatSeq(currentToken);

        seq.add( statement() );
        expect(Token.Kind.SEMICOLON);

        while( have( NonTerminal.STATEMENT ) ) {
            seq.add( statement() );
            expect(Token.Kind.SEMICOLON);
        }

        return seq;
    }

    private AST statement( ) {
        if( have( NonTerminal.ASSIGN ) ) {
            return assign();
        }
        else if( have( Token.Kind.CALL ) ) {
            return funcCall();
        }
        else if( have( Token.Kind.IF ) ) {
            return ifStat();
        }
        else if( have( Token.Kind.WHILE ) ) {
            return whileStat();
        }
        else if( have( Token.Kind.REPEAT ) ) {
            return repeatStat();
        }
        else if( have( Token.Kind.RETURN ) ) {
            return returnStat();
        }
        else {
            String err = reportSyntaxError(NonTerminal.STATEMENT);
            throw new Interpreter.QuitParseException("No executable statement: " + err );
        }
    }


    private FuncDecl funcDecl () {
        Token ftok = expectRetrieve( Token.Kind.FUNC );
        Token funcName = expectRetrieve(Token.Kind.IDENT);


        ArrayList< Symbol > argSymbols = formalParam();
        expect( Token.Kind.COLON );
        ArrayType returnType;
        if( accept( Token.Kind.VOID ) ) {
            returnType = new ArrayType(Token.Kind.VOID);
        }
        else {
            returnType = paramType();
        }

        ArrayList< ArrayType > params = new ArrayList<>();
        for( Symbol sym : argSymbols ) {
            params.add( sym.type() );
        }

        ArrayType funcType = ArrayType.makeFunctionType(returnType, params );
        // Symbol funcSym = new FunctionSymbol(funcName.lexeme(), funcType);

        Symbol funcSym = tryDeclareFunction(funcName, funcType);


        enterScope();

        for( Symbol sym : argSymbols ) {
            tryDeclareVariable(sym.name(), sym);
        }

        FuncBody body = funcBody();
        FuncDecl decl = new FuncDecl(funcName, funcType, body);
        decl.setArgs(argSymbols);

        exitScope();

        return decl;
    }

    private ArrayType paramType() {
        Token type = expectRetrieve(NonTerminal.TYPE);
        ArrayList<Integer> dims = new ArrayList<>();
        while( accept( Token.Kind.OPEN_BRACKET ) ) {
            expect( Token.Kind.CLOSE_BRACKET );
            dims.add(-1);
        }

        if( dims.isEmpty() ) {
            return new ArrayType(type, null );
        }
        return new ArrayType(type, dims);

    }

    private Symbol paramDecl() {
        ArrayType type = paramType();
        Token param = expectRetrieve( Token.Kind.IDENT );
        return new VariableSymbol(param.lexeme(), type);
    }

    private ArrayList< Symbol > formalParam() {

        ArrayList<Symbol> params = new ArrayList<>();
        expect(Token.Kind.OPEN_PAREN);

        if( have( NonTerminal.PARAM_DECL ) ) {
            params.add( paramDecl() );
            while( accept( Token.Kind.COMMA ) ) {
                params.add( paramDecl() );
            }
        }
        expect( Token.Kind.CLOSE_PAREN );

        return params;
    }

    private FuncBody funcBody( ) {
        Token brace = expectRetrieve( Token.Kind.OPEN_BRACE );
        DeclarationList vars = null;

        if( have(NonTerminal.VAR_DECL ) ) {
            vars = new DeclarationList(currentToken);

        }

        while( have( NonTerminal.VAR_DECL ) ) {
            ArrayList<VariableDeclaration> decls = varDecl();
            vars.addAll( decls );
        }

        StatSeq seq = statSeq();

        expect( Token.Kind.CLOSE_BRACE );
        expect( Token.Kind.SEMICOLON );

        return new FuncBody(brace, vars, seq);
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

            ast.setVars(list);
        }

        if( have( NonTerminal.FUNC_DECL ) ) {
            DeclarationList list = new DeclarationList(currentToken);
            while (have(NonTerminal.FUNC_DECL) ) {
                list.add( funcDecl() );
            }
            ast.setFuncs(list);
        }

        expect(Token.Kind.OPEN_BRACE);

        ast.setSeq( statSeq() );

        expect(Token.Kind.CLOSE_BRACE);
        expect(Token.Kind.PERIOD);

        // // Example PrintVisitor usage
        // PrintVisitor visitor = new PrintVisitor();
        // visitor.visit(ast);
        // System.err.println(visitor);

        return ast;
    }
}
