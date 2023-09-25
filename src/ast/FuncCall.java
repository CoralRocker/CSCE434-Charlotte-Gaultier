package ast;

import coco.Symbol;
import coco.Token;

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
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s\n", this));

        if( args != null ) {
            String[] lines = args.printPreOrder().split(System.lineSeparator());
            for( String line : lines ) {
                builder.append(String.format("\t%s\n", line));
            }
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return String.format("FunctionCall[%s:%s]", func.name(), func.type());
    }

    @Override
    public int lineNumber() {
        return 0;
    }

    @Override
    public int charPosition() {
        return 0;
    }
}
