package ast;

import coco.Token;

public class Assignment extends AST {

    public Designator target;
    public AST rvalue;

    public Assignment(Token tkn, Designator trgt, AST rval ) {
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
        builder.append(String.format("  %s\n", target));
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
