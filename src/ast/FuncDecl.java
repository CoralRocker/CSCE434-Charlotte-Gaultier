package ast;

import coco.ArrayType;
import coco.FunctionSymbol;
import coco.Symbol;
import coco.Token;
import types.*;

import java.util.ArrayList;

public class FuncDecl extends AST {

    private FunctionSymbol sym;
    private ArrayType declType;
    private ArrayList<Symbol> argList;
    private FuncBody body;
    public FuncDecl(Token tkn, ArrayType declType, FuncBody body, FunctionSymbol symbol ) {
        super(tkn);
        this.declType = declType;
        this.body = body;
        this.argList = new ArrayList<>();
        this.sym = symbol;
    }

    public String funcName(){
        return tkn.lexeme();
    }

    public void setArgs( ArrayList<Symbol> list) {
        this.argList = list;
    }

    public void add( Symbol arg ) {
        getArgList().add( arg );
    }

    // public Symbol symbol() {
    //     return getSym();
    // }

    @Override
    public String type() {
        return null;
    }

    public FunctionSymbol getSymbol(){
        return (FunctionSymbol) sym;
    }

    @Override
    public Type typeClass() { return sym.getReturnType(); }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("%s\n", this));

        for( String line : getBody().preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        return builder.toString();
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean isConstEvaluable() {
        return false;
    }

    @Override
    public AST constEvaluate() {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("FunctionDeclaration[%s:%s]", super.token().lexeme(), declType));
        // for( Symbol arg : argList ) {
        //     builder.append(String.format(", %s", arg));
        // }
        // builder.append("]");
        return builder.toString();
    }

    // public Symbol getSym() {
    //     return sym;
    // }

    public ArrayList<Symbol> getArgList() {
        return argList;
    }

    public FuncBody getBody() {
        return body;
    }
}
