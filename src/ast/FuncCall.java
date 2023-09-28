package ast;

import coco.Symbol;
import coco.Token;
import types.Type;

public class FuncCall extends AST {

    private Symbol func;
    private ArgList args;

    public FuncCall(Token tkn, Symbol func) {
        super(tkn);
        this.func = func;
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
        return null;
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
        return String.format("FunctionCall[%s:%s]", getFunc().name(), getFunc().type());
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

    public Symbol getFunc() {
        return func;
    }

    public ArgList getArgs() {
        return args;
    }
}
