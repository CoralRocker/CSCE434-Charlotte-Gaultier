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

    @Override
    public String type() {
        return null;
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
    public String toString() {
        return String.format("FloatLiteral[%f]", getLiteral());
    }

    public float getLiteral() {
        return literal;
    }
}
