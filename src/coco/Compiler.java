package coco;


import java.util.*;

import ast.*;
import ir.IRGenerator;
import ir.cfg.CodeGen.CodeGenerator;
import ir.cfg.CodeGen.DLXCode;
import ir.cfg.optimizations.*;
import ir.cfg.CFG;
import ir.cfg.registers.RegisterAllocator;
import org.apache.commons.cli.CommandLine;

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

    public String optimization(List<String> optArguments, CommandLine cmd) {
        for( CFG cfg : flowGraphs ) {
            for (String opt : optArguments) {
                System.out.printf("Running opt: %s\n", opt);
                switch (opt) {
                    case "cf" -> {
                        ReachingDefinition def = new ReachingDefinition(cfg, false, true, false, false);
                    }
                    case "cp" -> {
                        ReachingDefinition def = new ReachingDefinition(cfg, true, false, false, false);
                    }
                    case "cse" -> {
                        AvailableExpression expr = new AvailableExpression(cfg, true, false);
                    }

                    case "cpp" -> {
                        // ReachingDefinition def = new ReachingDefinition(cfg, false, false, true, true);
                        AvailableExpression expr = new AvailableExpression(cfg, false, true);
                    }
                    case "dce" -> {
                        Liveness live = new Liveness(cfg, true, true);
                    }
                    case "max" -> {
                        boolean changed = true;

                        int iter = 1;
                        while (changed) {
                            changed = false;

                            ProgramPointLiveness lvanal = new ProgramPointLiveness(cfg);
                            lvanal.calculate(false);
                            changed |= lvanal.doDCE(false);

                            ReachingDefinition def = new ReachingDefinition(cfg, true, true, false, false);
                            changed |= def.cfgchanged;

                            AvailableExpression avail = new AvailableExpression(cfg, true, true);
                            changed |= avail.isChanged();

                            // Liveness lvanal = new Liveness(cfg, false, true);
                            // changed |= lvanal.isChanged();

                        }
                    }
                }
                System.out.println("Post Optimization:");
                System.out.println(cfg.asDotGraph());
            }
        }
        return flowGraphs.get(flowGraphs.size()-1).asDotGraph();
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

    private List<CFG> flowGraphs;

    // Need to map from IDENT to memory offset

    public Compiler (Scanner scanner, int numRegs) {
        this.scanner = scanner;
        currentToken = this.scanner.next();
        numDataRegisters = numRegs;
        instructions = new ArrayList<>();
        ast = null;
    }

    public void regAlloc(int n) {
        CFG main = flowGraphs.get(flowGraphs.size()-1);
        RegisterAllocator allocator = new RegisterAllocator(n);
        allocator.allocateRegisters(main);
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
            return ast;
        }
        catch( Exception e ) {
            System.out.println("CAUGHT ERROR: " + e);
        }

        UnresolvedFunctionVisitor visitor = new UnresolvedFunctionVisitor();
        visitor.visit(ast);
        if( !visitor.errors().isEmpty() ) {
            for( Token err : visitor.errors() ) {
                reportResolveSymbolError(err.lexeme(), err.lineNumber(), err.charPosition());
            }
        }

        return ast;
    }

    public CFG genSSA(AST root) {
        IRGenerator gen = new IRGenerator();

        gen.visit((RootAST) root);

        flowGraphs = gen.getAllCFGs();
        return gen.getMainCFG();
    }

    // WARNING: Assumption is that AST, SSA, and Optimizations have been performed
    public int[] compile () {
        initSymbolTable();
        try {
            genCode();
            return instructions.stream().mapToInt(Integer::intValue).toArray();
        }
        catch (QuitParseException q) {
            errorBuffer.append("SyntaxError(" + lineNumber() + "," + charPosition() + ")");
            errorBuffer.append("[Could not complete parsing.]");
            return new ArrayList<Integer>().stream().mapToInt(Integer::intValue).toArray();
        }
    }

    public int[] genCode(){

        // TODO Generate code for functions (add below main code)
        List<DLXCode> assembly = CodeGenerator.generate(flowGraphs.get(flowGraphs.size()-1), numDataRegisters, true);

        instructions = new ArrayList<>();
        HashMap<String, Integer> funcMap = new HashMap<>();


        for( int cfg = 0; cfg < (flowGraphs.size()-1); cfg++ ) {
            CFG graph = flowGraphs.get(cfg);
            funcMap.put(graph.cfgID, assembly.size());
            List<DLXCode> func = CodeGenerator.generate(graph, numDataRegisters, false);

            assembly.addAll(func);
        }

        // Todo Resolve Functions
        var iter = assembly.listIterator();
        while( iter.hasNext() ) {
            DLXCode asm = iter.next();

            if( asm.getFormat() == DLXCode.FORMAT.UNRESOLVED_CALL ) {
                iter.set( DLXCode.jumpOp(DLXCode.OPCODE.JSR, funcMap.get(asm.getFuncSig())));
            }
        }


        System.out.printf("Instructions: \n");
        for( int i = 0; i < assembly.size(); i++ ) {
            System.out.printf("%3d : %-32s => 0x%08x\n", i, assembly.get(i).generateAssembly(), assembly.get(i).generateInstruction());
            instructions.add( assembly.get(i).generateInstruction() );
        }

        // converting from List<Integer> to int[]. There has got to be a better way to do this...
        int[] instrReturn = new int[instructions.size()];
        for(int i = 0; i < instrReturn.length; i++) {
            instrReturn[i] = instructions.get(i);
        }

        return instrReturn;
    }

    // SymbolTable Management =====================================================

    private void initSymbolTable () {

        currentSymbolTable = new SymbolTable(null);

        // Add Default Function Declarations
        ArrayType readInt = ArrayType.makeFunctionType(
                new ArrayType( Token.Kind.INT ),
                new ArrayList<>()
        );
        currentSymbolTable.insert("readInt", new FunctionSymbol("readInt", readInt, new ArrayType(Token.Kind.INT)));

        ArrayType readFloat = ArrayType.makeFunctionType(
                Token.Kind.FLOAT
        );
        currentSymbolTable.insert("readFloat", new FunctionSymbol("readFloat", readFloat, new ArrayType(Token.Kind.FLOAT)));

        ArrayType readBool = ArrayType.makeFunctionType(
                Token.Kind.BOOL
        );
        currentSymbolTable.insert("readBool", new FunctionSymbol("readBool", readBool, new ArrayType(Token.Kind.BOOL)));

        ArrayType printInt = ArrayType.makeFunctionType(
                Token.Kind.VOID,
                new Token.Kind[]{Token.Kind.INT}
        );
        currentSymbolTable.insert("printInt", new FunctionSymbol("printInt", printInt, new ArrayType(Token.Kind.VOID)));

        ArrayType printFloat = ArrayType.makeFunctionType(
                Token.Kind.VOID,
                new Token.Kind[]{Token.Kind.FLOAT}
        );
        currentSymbolTable.insert("printFloat", new FunctionSymbol("printFloat", printFloat, new ArrayType(Token.Kind.VOID)));

        ArrayType printBool = ArrayType.makeFunctionType(
                Token.Kind.VOID,
                new Token.Kind[]{Token.Kind.BOOL}
        );
        currentSymbolTable.insert("printBool", new FunctionSymbol("printBool", printBool, new ArrayType(Token.Kind.VOID)));

        ArrayType println = ArrayType.makeFunctionType(
                Token.Kind.VOID
        );
        currentSymbolTable.insert("println", new FunctionSymbol("println", println, new ArrayType(Token.Kind.VOID)));

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

    private FunctionSymbol tryDeclareFunction(Token func, ArrayType type) {
        Symbol sym = null;
        if( currentSymbolTable.contains(func) ) {
            sym = currentSymbolTable.lookup(func);

            if( ! (sym instanceof FunctionSymbol) ) {
                throw new RuntimeException(String.format("Function %s is not a function identifier!", func.lexeme()));
            }
        }
        else {
            sym = currentSymbolTable.insert(func, new FunctionSymbol(func, type) );
        }

        FunctionSymbol funcSym = (FunctionSymbol) sym;
        if( funcSym.contains(type) ) {
            reportDeclareSymbolError(func.lexeme(), func.lineNumber(), func.charPosition());
        }
        else {
            funcSym.add(type);
        }

        return funcSym;
    }

    private FunctionSymbol tryResolveFunction(Token func) {
        Symbol sym = null;
        if( !currentSymbolTable.contains(func) ) {
            SymbolTable global = currentSymbolTable.globalScope(0);
            return (FunctionSymbol) global.insert(func, new FunctionSymbol(func));
        }
        sym = currentSymbolTable.lookup(func);
        if( ! (sym instanceof FunctionSymbol) ) {
            throw new RuntimeException(String.format("Function call %s is not a function! (%s)", func, sym));
        }

        return (FunctionSymbol) sym;
    }

    private Symbol tryDeclareVariableStr (String str, Symbol var) {

        try{
            return currentSymbolTable.insert(str, var);
        }
        catch(RedeclarationError e){
            // TODO this is causing the line num errors. require this to take a token not a string and use the line num char pos of the token
            reportDeclareSymbolError(str, lineNumber(), charPosition());
        }
        //TODO: Try declaring variable, handle RedeclarationError
        return new VariableSymbol("ERROR", new ArrayType(Token.Kind.ERROR));

    }

    private void reportResolveSymbolError (String name, int lineNum, int charPos) {
        //if( errorBuffer.isEmpty() ) {
        //    errorBuffer.append("Error parsing file.\n");
        //}
        String message = "ResolveSymbolError(" + lineNum + "," + charPos + ")[Could not find " + name + ".]";
        errorBuffer.append(message + "\n");
//        throw new QuitParseException(message);
    }

    private void reportDeclareSymbolError (String name, int lineNum, int charPos) {
        //if( errorBuffer.isEmpty() ) {
        //    errorBuffer.append("Error parsing file.\n");
        //}
        String message = "DeclareSymbolError(" + lineNum + "," + charPos + ")[" + name + " already exists.]";
        errorBuffer.append(message + "\n");
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
        Stack<Token> startBrackets = new Stack<>();
        while( have( Token.Kind.OPEN_BRACKET ) ) {
            Token sb = expectRetrieve(Token.Kind.OPEN_BRACKET);
            AST dim = relExpr();
            Token eb = expectRetrieve( Token.Kind.CLOSE_BRACKET );

            startBrackets.push(eb);

            indexes.add( dim );
        }

        for( int i = 0; i < indexes.size(); i++ ) {
            symbol = new ArrayIndex(indexes.get(i).token(), startBrackets.firstElement(), symbol, indexes.get(i));
            startBrackets.remove(0);
        }
        // for( int i = indexes.size()-1; i >= 0; i-- ) {
        //     symbol = new ArrayIndex(indexes.get(i).token(), startBrackets.pop(), symbol, indexes.get(i));
        // }

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
            throw new QuitParseException(err);
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
        tryDeclareVariable(ident, decl.symbol());

        while( accept(Token.Kind.COMMA ) ) {
            ident = expectRetrieve( Token.Kind.IDENT );
            decl = new VariableDeclaration(ident, new VariableSymbol(ident.lexeme(), arrtype) );
            tryDeclareVariable(ident, decl.symbol());
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
        FuncCall function = new FuncCall(call, sym, func);

        ArrayList< AST > arguments = new ArrayList<>();
        ArgList list = new ArgList(currentToken);
        function.setArgs(list);
        if( !have( Token.Kind.CLOSE_PAREN ) ) {
            list.add(relExpr());
            while ( accept(Token.Kind.COMMA)) {
                list.add(relExpr());
            }
        }
        function.setEndParen(expectRetrieve(Token.Kind.CLOSE_PAREN));

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
                    throw new QuitParseException("Unable to parse unary op \"%s\": %s".formatted(op.lexeme(), err));
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
        AST bool = relation();
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
            throw new QuitParseException(err);
        }

        StatSeq seq = new StatSeq(currentToken);

        AST stat = statement();
        seq.add( stat );
        expect(Token.Kind.SEMICOLON);

        while( have( NonTerminal.STATEMENT ) ) {
            stat = statement();
            seq.add( stat );
            if(stat instanceof Return){
                seq.setReturnType(stat.typeClass());
            }
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

        FunctionSymbol funcSym = tryDeclareFunction(funcName, funcType);

        enterScope();

        for( Symbol sym : argSymbols ) {
            tryDeclareVariableStr(sym.name(), sym);
        }

        FuncBody body = funcBody();
        FuncDecl decl = new FuncDecl(funcName, funcType, body, funcSym);
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

//         TypeChecker usage
//        TypeChecker visitor = new TypeChecker();
//        visitor.visit(ast);
//        System.err.println(visitor.errorReport());

        return ast;
    }
}
