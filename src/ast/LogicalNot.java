package ast;

import coco.Token;
import types.Type;

public class LogicalNot extends AST {
    private AST rvalue;
    public AST getRvalue() { return rvalue; }
    public LogicalNot(Token tkn, AST expr) {
        super(tkn);
        rvalue = expr;
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public Type typeClass() {
        return this.type;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder();
        builder.append(this);
        builder.append("\n");
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
        return "LogicalNot";
    }
}
