package ast;

import coco.Token;

public class Subtraction extends AST {
    public Token op;
    public AST rvalue, lvalue;

    public Subtraction(Token tkn, AST lvalue, AST rvalue) {
        super(tkn);
        this.lvalue = lvalue;
        this.rvalue = rvalue;
        this.op = tkn;
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder();
        builder.append(this);
        builder.append("\n");

        for( String line : lvalue.preOrderLines() ) {
            builder.append(String.format("\t%s\n", line));
        }
        for( String line : rvalue.preOrderLines() ) {
            builder.append(String.format("\t%s\n", line));
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return String.format("Subtraction[%s]", op.lexeme());
    }
}
