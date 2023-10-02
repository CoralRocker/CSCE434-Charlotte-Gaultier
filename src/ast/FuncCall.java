package ast;

import coco.FunctionSymbol;
import coco.Symbol;
import coco.Token;
import types.*;

public class FuncCall extends AST {

    protected FunctionSymbol func;
    private ArgList args;

    protected Token funcTok;

    public FuncCall(Token tkn, FunctionSymbol func, Token funcTok) {
        super(tkn);
        this.func = func;
        this.funcTok = funcTok;
    }

    public void setArgs( ArgList args ) {
        this.args = args;
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public Type typeClass() {
        return func.getRealReturnType();
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s\n", this));

        if( getArgs() != null ) {
            String[] lines = getArgs().printPreOrder().split(System.lineSeparator());
            for( String line : lines ) {
                builder.append(String.format("  %s\n", line));
            }
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        if( func.typeSignatures().isEmpty() ) {
            return String.format("FunctionCall[%s]", func.name());
        }
        else {
            return String.format("FunctionCall[%s]", func.typeSignatures());
        }
    }

    @Override
    public int lineNumber() {
        return 0;
    }

    @Override
    public int charPosition() {
        return 0;
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

    public Symbol getFunc() {
        return func;
    }

    public ArgList getArgs() {
        return args;
    }
}
