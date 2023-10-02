package ast;

import coco.Token;
import types.FloatType;
import types.Type;

public class FloatLiteral extends AST {
    private float literal;
    public FloatLiteral(Token lit) {
        super(lit);
        literal = Float.parseFloat(lit.lexeme());
    }

    public FloatLiteral(Token lit, float val) {
        super(lit);
        literal = val;
    }

    @Override
    public String type() {
        return "FloatLiteral";
    }

    @Override
    public Type typeClass() {
        return new FloatType();
    }

    @Override
    public String printPreOrder() {
        return this.toString() + "\n";
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean isConstEvaluable() {
        return true;
    }

    @Override
    public AST constEvaluate() {
        return this;
    }

    @Override
    public String toString() {
        return String.format("FloatLiteral[%f]", getLiteral());
    }

    public double getLiteral() {
        return literal;
    }

    public float getFloatLiteral() { return literal; }
}
