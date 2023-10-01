package ast;

import coco.Token;
import types.Type;

public class LogicalOr extends AST {
    private AST rvalue;
    private AST lvalue;
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
    public Type typeClass() {
        return this.type;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder();
        builder.append(this);
        builder.append("\n");

        for( String line : getLvalue().preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        for( String line : getRvalue().preOrderLines() ) {
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

    public AST getRvalue() {
        return rvalue;
    }

    public AST getLvalue() {
        return lvalue;
    }

    @Override
    public boolean isConstEvaluable() {
        return lvalue.isConstEvaluable() && rvalue.isConstEvaluable();
    }

    @Override
    public AST constEvaluate() {
        BoolLiteral left = (BoolLiteral) lvalue.constEvaluate();
        BoolLiteral right = (BoolLiteral) rvalue.constEvaluate();

        return new BoolLiteral(super.token(), left.getBoolLiteral() || right.getBoolLiteral());
    }
}
