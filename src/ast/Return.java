package ast;

import coco.Token;
import types.*;

public class Return extends AST {
    private AST retval = null;
    public AST getReturn() { return retval; }
    public Return(Token tkn) {
        super(tkn);
    }

    public Return( Token tkn, AST val ) {
        super( tkn );
        retval = val;
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public Type typeClass() {
        if (retval == null){
            return new VoidType();
        }
        return type;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder(this + "\n");
        if( retval == null ) {
            return builder.toString();
        }

        for( String line : retval.preOrderLines() ) {
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
        return "ReturnStatement";
    }
}
