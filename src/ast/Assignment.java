package ast;

import coco.Token;

public class Assignment extends AST {

    public AST target;
    public AST rvalue;

    public Assignment(Token tkn, AST trgt, AST rval ) {
        super(tkn);
        target = trgt;
        rvalue = rval;
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s\n", this));
        for( String line : target.preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }
        for( String line : rvalue.preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return "Assignment";
    }
}
