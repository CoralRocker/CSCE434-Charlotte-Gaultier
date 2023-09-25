package ast;

import coco.Token;

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
    public String printPreOrder() {
        return this.toString() + "\n";
    }

    @Override
    public String toString() {
        return String.format("FloatLiteral[%f]", literal);
    }
}
