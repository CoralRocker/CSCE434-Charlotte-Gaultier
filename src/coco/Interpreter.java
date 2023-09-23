package coco;

import javax.print.attribute.standard.NumberUp;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;

public class Interpreter {

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

    protected int lineNumber () {
        return currentToken.lineNumber();
    }

    protected int charPosition () {
        return currentToken.charPosition();
    }

    private String lexeme () {
        return currentToken.lexeme();
    }

// Interpreter ============================================================
    private Scanner scanner;
    private Token currentToken;

    private BufferedReader reader;
    private StringTokenizer st;

    // Holds the variables for each symbol / array position
    private HashMap< String, Variable > variables;

    // Holds type information for each symbol
    private HashMap< String, ArrayType > symbols;

    private boolean earlyReturn = false;

    public Interpreter (Scanner scanner, InputStream in) {
        this.scanner = scanner;
        currentToken = this.scanner.next();

        reader = new BufferedReader(new InputStreamReader(in));
        st = null;

        variables = new HashMap<>();
        symbols = new HashMap<>();
    }

    public void interpret () {
        try {
            computation();
        }
        catch (QuitParseException q) {
            // too verbose
            // errorBuffer.append("SyntaxError(" + lineNumber() + "," + charPosition() + ")");
            // errorBuffer.append("[Could not complete parsing.]");
        }
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

// Pre-defined Functions ======================================================
    private String nextInput () {
        while (st == null || !st.hasMoreElements()) {
            try {
                st = new StringTokenizer(reader.readLine());
            }
            catch (IOException e) {
                throw new QuitParseException("Interepter: Couldn't read data file\n" + e.getMessage());
            }
        }
        return st.nextToken();
    }

    private int readInt () {
        System.out.print("int? ");
        return Integer.parseInt(nextInput());
    }

    private float readFloat () {
        System.out.print("float? ");
        return Float.parseFloat(nextInput());
    }

    private boolean readBool () {
        System.out.print("true or false? ");
        return Boolean.parseBoolean(nextInput());
    }

    private void printInt (int x) {
        System.out.print(x + " ");
    }

    private void printFloat (float x) {
        System.out.printf("%.2f ",x);
    }

    private void printBool (boolean x) {
        System.out.print(x + " ");
    }

    private void println () {
        System.out.println();
    }

// Grammar Rules ==============================================================

    // function for matching rule that only expects nonterminal's FIRST set
    private Token matchNonTerminal (NonTerminal nt) {
        return expectRetrieve(nt);
    }

    // TODO: implement operators and type grammar rules
    private void type() {
        expect( NonTerminal.TYPE );
    }

    // literal = integerLit | floatLit
    private Token literal () {
        return matchNonTerminal(NonTerminal.LITERAL);
    }

    // designator = ident { "[" relExpr "]" }
    private Variable designator ( boolean execute ) {
        int lineNum = lineNumber();
        int charPos = charPosition();

        Token ident = expectRetrieve(Token.Kind.IDENT);
        ArrayList< Integer > dims = null;

        while( accept( Token.Kind.OPEN_BRACKET ) ) {
            Variable dim = relExpr( execute && !earlyReturn );
            if( ! dim.isInt() ) {
                throw new QuitParseException("Expected integer as index for array, but got %s. Line %d Pos %d".formatted(dim.getType(), lineNumber(), charPosition() ) );
            }
            if( dims == null ) {
                dims = new ArrayList<>();
            }
            dims.add( dim.getInt() );
            expect( Token.Kind.CLOSE_BRACKET );
        }

        if( execute && !earlyReturn ) {
            ArrayType typeInfo = symbols.getOrDefault(ident.lexeme(), null);
            if (typeInfo == null) {
                reportSyntaxError(NonTerminal.STATEMENT);
                throw new QuitParseException("Identifier \"%s\" Does not exist. Line %d Pos %d".formatted(ident.lexeme(), lineNumber(), charPosition()));
            }

            String uniqueIdent = typeInfo.at(ident, dims);

            Variable var = variables.getOrDefault(uniqueIdent, null);
            if (var == null) {
                throw new RuntimeException("Unique identifier \"%s\" does not exist in the variables map.".formatted(uniqueIdent));
            }

            return var;
        }
        else {
            return null;
        }
    }

    private Variable relation( boolean execute ) {
        expect( Token.Kind.OPEN_PAREN );
        Variable var = relExpr( execute && !earlyReturn );
        expect( Token.Kind.CLOSE_PAREN );

        if( var == null ) {
            return new Variable(false);
        }
        return var;
    }

    private Variable funcCall( boolean execute ) {
        expect( Token.Kind.CALL );
        Token func = expectRetrieve( Token.Kind.IDENT );
        expect( Token.Kind.OPEN_PAREN );

        if( accept( Token.Kind.CLOSE_PAREN ) ) {
            if( execute && !earlyReturn ){
                switch( func.lexeme() ) {
                    case "readInt" -> {
                        return new Variable( readInt() );
                    }
                    case "readFloat" -> {
                        return new Variable( readFloat() );
                    }
                    case "readBool" -> {
                        return new Variable( readBool() );
                    }
                    case "println" -> {
                        println();
                        return new Variable();
                    }
                    default -> {
                        reportSyntaxError(Token.Kind.CLOSE_PAREN);
                        throw new QuitParseException("Function call \"%s\" either is not a real function or requires more than 0 arguments.".formatted(func.lexeme()));
                    }
                }
            }
        }
        else {
            ArrayList< Variable > arguments = new ArrayList<>();
            arguments.add( relExpr( execute && !earlyReturn ) );
            while( accept( Token.Kind.COMMA ) ) {
                arguments.add( relExpr( execute && !earlyReturn ) );
            }
            expect( Token.Kind.CLOSE_PAREN );

            if ( execute && !earlyReturn ) {
                if( arguments.size() > 1 ) {
                    throw new RuntimeException("Multi-argument functions are not yet implemented!");
                }

                switch( func.lexeme() ) {
                    case "printInt" -> {
                        printInt( arguments.get(0).getInt() );
                        return new Variable();
                    }
                    case "printFloat" -> {
                        printFloat( arguments.get(0).getFloat() );
                        return new Variable();
                    }
                    case "printBool" -> {
                        printBool( arguments.get(0).getBool() );
                        return new Variable();
                    }
                    default -> {
                        reportSyntaxError( Token.Kind.CLOSE_PAREN );
                        throw new QuitParseException("Function call \"%s\" does not exist".formatted(func.lexeme()) );
                    }
                }

            }

        }

        // Reached if not executing
        return null;
    }

    private Variable relExpr( boolean execute ) {
        Variable var = addExpr( execute && !earlyReturn );

        while( have( NonTerminal.REL_OP ) ) {
            Token op = expectRetrieve( NonTerminal.REL_OP );
            Variable rval =  addExpr( execute && !earlyReturn );

            if( execute && !earlyReturn ) {
                switch (op.kind()) {
                    case EQUAL_TO -> {
                        var = var.equals(rval);
                    }
                    case NOT_EQUAL -> {
                        var = var.notEquals(rval);
                    }
                    case GREATER_EQUAL -> {
                        var = var.greaterThan(rval, true);
                    }
                    case GREATER_THAN -> {
                        var = var.greaterThan(rval, false);
                    }
                    case LESS_EQUAL -> {
                        var = var.lessThan(rval, true);
                    }
                    case LESS_THAN -> {
                        var = var.lessThan(rval, false);
                    }
                }
            }
        }

        return var;
    }

    private Variable addExpr( boolean execute ) {
        Variable var = multExpr( execute && !earlyReturn );

        while( have( NonTerminal.ADD_OP )
            || ( have( NonTerminal.LITERAL ) && currentToken.lexeme().startsWith("-") ) ) {
            Token op;
            Variable rval;
            if( have( NonTerminal.LITERAL ) && currentToken.lexeme().startsWith("-") ) {
                if( currentToken.isKind(Token.Kind.INT_VAL) ) {
                    rval = new Variable( -1 * Integer.parseInt(currentToken.lexeme()) );
                }
                else {
                    rval = new Variable( -1.f * Float.parseFloat(currentToken.lexeme()) );
                }

                var = var.sub( rval );
                accept(NonTerminal.LITERAL);
            }
            else {
                op = expectRetrieve(NonTerminal.ADD_OP);
                rval = multExpr(execute && !earlyReturn);

                if (execute && !earlyReturn) {
                    switch (op.kind()) {
                        case ADD -> {
                            var = var.add(rval);
                        }
                        case SUB -> {
                            var = var.sub(rval);
                        }
                        case OR -> {
                            var = var.or(rval);
                        }
                    }
                }
            }
        }

        return var;
    }

    private Variable multExpr( boolean execute ) {
        Variable var = powExpr( execute && !earlyReturn );

        while( have( NonTerminal.MUL_OP ) ) {
            Token op = expectRetrieve( NonTerminal.MUL_OP );
            Variable rval = powExpr( execute && !earlyReturn );

            if( execute && !earlyReturn ) {
                switch (op.kind()) {
                    case MUL -> {
                        var = var = var.mult(rval);
                    }
                    case DIV -> {
                        var = var.div(rval);
                    }
                    case AND -> {
                        var = var.and(rval);
                    }
                    case MOD -> {
                        var = var.mod(rval);
                    }
                }
            }
        }

        return var;
    }

    private Variable powExpr( boolean execute ) {
        Variable var = groupExpr( execute && !earlyReturn );

        while( accept( Token.Kind.POW ) ) {
            Variable rval = groupExpr( execute && !earlyReturn );

            if( execute && !earlyReturn ) {
                var = var.pow(rval);
            }
        }

        return var;
    }

    private Variable groupExpr( boolean execute ) {
        if( have( NonTerminal.LITERAL ) ) {
            Token lit = expectRetrieve( NonTerminal.LITERAL );
            return new Variable(lit);
        }
        else if( have( Token.Kind.IDENT ) ) {
            return designator( execute && !earlyReturn );
        }
        else if( accept( Token.Kind.NOT ) ) {
            Variable var = relExpr( execute && !earlyReturn );
            return new Variable( ! var.getBool() );
        }
        else if( have( Token.Kind.OPEN_PAREN ) ) {
            Variable var = relation( execute && !earlyReturn );
            return var;
        } else if (have(Token.Kind.CALL) ) {
            return funcCall( execute && !earlyReturn );
        }
        else {
            String err = reportSyntaxError( NonTerminal.GROUP_EXPR );
            throw new QuitParseException(err);
        }
    }

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

    private void assign( boolean execute ) {
        Variable var = designator( execute && !earlyReturn );
        Token op;

        if( have( NonTerminal.UNARY_OP ) ) {
            op = expectRetrieve( NonTerminal.UNARY_OP );

            if( execute && !earlyReturn ) {
                switch (op.lexeme()) {
                    case "++" -> {
                        var.set(var.getInt() + 1);
                    }
                    case "--" -> {
                        var.set(var.getInt() - 1);
                    }
                    default -> {
                        String err = reportSyntaxError(NonTerminal.UNARY_OP);
                        throw new QuitParseException("Unable to parse unary op \"%s\": %s".formatted(op.lexeme(), err));
                    }
                }
            }
        }
        else {
            op = expectRetrieve( NonTerminal.ASSIGN_OP );
            Variable rvalue = relExpr( execute && !earlyReturn );
            if( execute && !earlyReturn ) {
                switch (op.lexeme()) {
                    case "=" -> {
                        var.set(rvalue);
                    }
                    case "+=" -> {
                        var.set(var.add(rvalue));
                    }
                    case "-=" -> {
                        var.set(var.sub(rvalue));
                    }
                    case "*=" -> {
                        var.set(var.mult(rvalue));
                    }
                    case "/=" -> {
                        var.set(var.div(rvalue));
                    }
                    case "^=" -> {
                        var.set(var.pow(rvalue));
                    }
                    case "%=" -> {
                        var.set(var.mod(rvalue));
                    }
                }
            }
        }
    }

    private void varDecl () {
        ArrayType arrtype = typeDecl(); // Array Identifier Generator
        Token.Kind type = arrtype.getType(); // Array Data Type
        Token ident = expectRetrieve( Token.Kind.IDENT ); // Initial Identifier

        // Check if a symbol exists with the given identifier.
        if( symbols.containsKey( ident.lexeme() ) ) {
            reportSyntaxError( ident.kind() );
            throw new QuitParseException("Duplicate identifier declaration: \"%s\" on line %d, char %d".formatted(ident.lexeme(), ident.lineNumber(), ident.charPosition()));
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
                throw new QuitParseException("Duplicate identifier declaration: \"%s\" on line %d, char %d".formatted(ident.lexeme(), ident.lineNumber(), ident.charPosition()));
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


    private void ifStat( boolean execute ) {
        expect( Token.Kind.IF );
        Variable bool = relation( execute && !earlyReturn );
        expect( Token.Kind.THEN );

        if( execute && bool.getType() != Variable.Type.BOOL ) {
            reportSyntaxError(Token.Kind.THEN);
            throw new QuitParseException("IF statement does not contain a boolean type!: " + bool.getType());
        }


        boolean run = bool.getBool();
        statSeq( run && execute && !earlyReturn );

        if( accept( Token.Kind.ELSE ) ) {
            statSeq( !run && execute && !earlyReturn );
        }

        expect( Token.Kind.FI );
    }

    private void statSeq( boolean execute ) {
        if( ! have( NonTerminal.STATEMENT) ) {
            String err = reportSyntaxError(NonTerminal.STATEMENT);
            throw new QuitParseException(err);
        }

        statement( execute && !earlyReturn );
        expect(Token.Kind.SEMICOLON);

        while( have( NonTerminal.STATEMENT ) ) {
            statement( execute && !earlyReturn );
            expect(Token.Kind.SEMICOLON);
        }
    }

    private void funcDecl ( boolean execute ) {
        expect( Token.Kind.FUNC );
        Token funcName = expectRetrieve(Token.Kind.IDENT);
        formalParam();
        expect( Token.Kind.COLON );
        if( ! accept( Token.Kind.VOID ) ) {
            type();
        }

        funcBody( execute && !earlyReturn );

    }

    private void paramType() {
        type();
        while( accept( Token.Kind.OPEN_BRACKET ) ) {
            expect( Token.Kind.CLOSE_BRACKET );
        }
    }

    private void paramDecl() {
        paramType();
        Token param = expectRetrieve( Token.Kind.IDENT );
    }

    private void formalParam() {
        expect(Token.Kind.OPEN_PAREN);

        if( have( NonTerminal.PARAM_DECL ) ) {
            paramDecl();
            while( accept( Token.Kind.COMMA ) ) {
                paramDecl();
            }
        }
        expect( Token.Kind.CLOSE_PAREN );
    }

    private void funcBody( boolean execute ) {
        expect( Token.Kind.OPEN_BRACE );
        while( have( NonTerminal.VAR_DECL ) ) {
            varDecl();
        }

        statSeq( execute && !earlyReturn );

        expect( Token.Kind.CLOSE_BRACE );
        expect( Token.Kind.SEMICOLON );

    }

    private void whileStat( boolean execute ) {
        Token wtok = expectRetrieve( Token.Kind.WHILE );
        Variable cond = relation( execute && !earlyReturn );
        expectRetrieve( Token.Kind.DO );
        statSeq(execute && cond.getBool().booleanValue() );

        if( cond.getBool().booleanValue() ) {
            scanner.backtrack(wtok);
            expect( Token.Kind.OD );
            whileStat(execute);
        }
        else {
            expect( Token.Kind.OD );
        }

    }

    private void repeatStat( boolean execute ) {
        expect( Token.Kind.REPEAT );
        statSeq( execute && !earlyReturn );
        // if( earlyReturn ) return;
        expect( Token.Kind.UNTIL );
        Variable cond = relation( execute && !earlyReturn );
    }

    private void returnStat( boolean execute ) {
        expect( Token.Kind.RETURN );

        if( have( NonTerminal.GROUP_EXPR ) ) {
            Variable retval = relExpr( execute && !earlyReturn );
        }

        // TODO: Skip Everything until end of times (or end of file)
        if( execute ) {
            earlyReturn = true;
        }
        // do {
        //     try {
        //         currentToken = scanner.next();
        //     }
        //     catch (NoSuchElementException e) {
        //         String errorMessage = reportSyntaxError( Token.Kind.EOF );
        //         throw new QuitParseException(errorMessage);
        //     }
        // }while( currentToken.kind() != Token.Kind.EOF );
    }

    private void statement( boolean execute ) {
        if( have( NonTerminal.ASSIGN ) ) {
            assign( execute && !earlyReturn );
        }
        else if( have( Token.Kind.CALL ) ) {
            funcCall( execute && !earlyReturn );
        }
        else if( have( Token.Kind.IF ) ) {
            ifStat( execute && !earlyReturn );
        }
        else if( have( Token.Kind.WHILE ) ) {
            whileStat( execute && !earlyReturn );
        }
        else if( have( Token.Kind.REPEAT ) ) {
            repeatStat( execute && !earlyReturn );
        }
        else if( have( Token.Kind.RETURN ) ) {
            returnStat( execute && !earlyReturn );
        }
        else {
            String err = reportSyntaxError(NonTerminal.STATEMENT);
            throw new QuitParseException("No executable statement: " + err );
        }
    }

    // TODO: implement remaining grammar rules

    // computation	= "main" {varDecl} {funcDecl} "{" statSeq "}" "."
    private void computation () {

        expect(Token.Kind.MAIN);

        while( have(NonTerminal.VAR_DECL) ) {
            varDecl();
        }

        while( have(NonTerminal.FUNC_DECL ) ) {
            funcDecl( false );
        }

        expect(Token.Kind.OPEN_BRACE);
        statSeq(true);
        // if( earlyReturn ) return;
        expect(Token.Kind.CLOSE_BRACE);
        expect(Token.Kind.PERIOD);
    }
}
