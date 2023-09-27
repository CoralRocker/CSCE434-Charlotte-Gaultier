package ast;

import coco.Token;

public class LogicalOr extends AST {
    private AST rvalue, lvalue;
    public LogicalOr(Token tkn, AST lval, AST rval) {
        super(tkn);
        rvalue = rval;
        lvalue = lval;
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
            builder.append(String.format("  %s\n", line));
        }

        for( String line : rvalue.preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        return builder.toString();
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LogicalOr";
    }
}
