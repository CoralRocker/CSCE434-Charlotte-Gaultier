package ast;

import coco.Token;
import types.BoolType;
import types.Type;

public class BoolLiteral extends AST {

    private boolean literal;

    public BoolLiteral(Token token, boolean b) {
        super(token);
        literal = b;
    }

    public double getLiteral() { if( literal) return 1.; else return 0.; }

    @Override
    public boolean getBoolLiteral() {
        return literal;
    }

    public BoolLiteral(Token tkn) {
        super(tkn);
        literal = Boolean.parseBoolean(tkn.lexeme());
    }

    @Override
    public String type() {
        return "BoolLiteral";
    }

    @Override
    public Type typeClass() {
        return new BoolType();
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
        return String.format("BoolLiteral[%s]", literal);
    }
}
