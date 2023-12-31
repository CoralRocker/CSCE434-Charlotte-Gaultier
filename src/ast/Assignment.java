package ast;

import coco.Token;
import types.Type;

public class Assignment extends AST {

    private AST target;
    private AST rvalue;

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
    public Type typeClass() {
        return type;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s\n", this));
        for( String line : getTarget().preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }
        for( String line : getRvalue().preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        return builder.toString();
    }

    @Override
    public <E> E accept(NodeVisitor<E> visitor) {
        return visitor.visit(this);
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
        return "Assignment";
    }

    public AST getTarget() {
        return target;
    }

    public AST getRvalue() {
        return rvalue;
    }
}
