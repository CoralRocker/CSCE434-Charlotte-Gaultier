package ast;

import coco.Token;
import types.Type;

public class Modulo extends AST {
    public Token op;
    private AST rvalue;
    private AST lvalue;

    public Modulo(Token tkn, AST lvalue, AST rvalue) {
        super(tkn);
        this.lvalue = lvalue;
        this.rvalue = rvalue;
        this.op = tkn;
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
        return String.format("Modulo");
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
        if( !isConstEvaluable() )
            return null;

        AST left = lvalue.constEvaluate();
        AST right = rvalue.constEvaluate();

        if( left instanceof FloatLiteral ) {
            FloatLiteral lval = (FloatLiteral) left;
            if( right instanceof IntegerLiteral ) {
                return new FloatLiteral(super.token(), (float) (lval.getLiteral() % ((IntegerLiteral)right).getLiteral()));
            }
            else {
                return new FloatLiteral(super.token(), (float) (lval.getLiteral() % ((FloatLiteral)right).getLiteral()));
            }
        }
        else {
            IntegerLiteral lval = (IntegerLiteral) left;
            if( right instanceof IntegerLiteral ) {
                return new IntegerLiteral(super.token(), (int) (lval.getLiteral() % ((IntegerLiteral)right).getLiteral()));
            }
            else {
                return new FloatLiteral(super.token(), (float) (lval.getLiteral() % ((FloatLiteral)right).getLiteral()));

            }
        }
    }
}
