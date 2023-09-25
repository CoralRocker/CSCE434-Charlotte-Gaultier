package ast;

import coco.Token;

public class LogicalNot extends AST {
    private AST rvalue;
    public LogicalNot(Token tkn, AST expr) {
        super(tkn);
        rvalue = expr;
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
        for( String line : rvalue.preOrderLines() ) {
            builder.append(String.format("\t%s\n", line));
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return "LogicalNot";
    }
}
