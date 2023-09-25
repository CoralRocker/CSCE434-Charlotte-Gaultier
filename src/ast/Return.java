package ast;

import coco.Token;

public class Return extends AST {
    private AST retval = null;
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
    public String toString() {
        return "ReturnStatement";
    }
}
