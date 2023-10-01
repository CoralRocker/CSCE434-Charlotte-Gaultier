package ast;

import coco.Token;
import types.Type;

public class Relation extends AST {

    private AST rvalue;
    private AST lvalue;
    private Token op;

    public Relation(Token tkn, AST lval, AST rval) {
        super(tkn);
        op = tkn;
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
        String[] lines = getLvalue().printPreOrder().split(System.lineSeparator());
        for( String line : lines ) {
            builder.append(String.format("  %s\n", line));
        }
        lines = getRvalue().printPreOrder().split(System.lineSeparator());
        for( String line : lines ) {
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
        return String.format("Relation[%s]", op.lexeme());
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
        AST left = lvalue.constEvaluate();
        AST right = rvalue.constEvaluate();

        switch( op.kind() ) {
            case EQUAL_TO -> {
                return new BoolLiteral(super.token(), left.getLiteral() == right.getLiteral());

            }
            case NOT_EQUAL -> {
                return new BoolLiteral(super.token(), left.getLiteral() != right.getLiteral());
            }

            case GREATER_EQUAL -> {
                return new BoolLiteral(super.token(), left.getLiteral() >= right.getLiteral());
            }
            case GREATER_THAN -> {
                return new BoolLiteral(super.token(), left.getLiteral() > right.getLiteral());
            }
            case LESS_EQUAL -> {
                return new BoolLiteral(super.token(), left.getLiteral() <= right.getLiteral());
            }

            case LESS_THAN -> {
                return new BoolLiteral(super.token(), left.getLiteral() < right.getLiteral());
            }
        }

        return null;
    }
}
