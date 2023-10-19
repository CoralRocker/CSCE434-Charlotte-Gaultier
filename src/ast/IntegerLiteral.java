package ast;

import coco.Token;
import types.IntType;
import types.Type;

public class IntegerLiteral extends AST {

    private int literal;

    public IntegerLiteral(Token tkn) {
        super(tkn);
        literal = Integer.parseInt(tkn.lexeme());
    }

    public IntegerLiteral(Token token, int i) {
        super(token);
        literal = i;
    }

    @Override
    public String type() {
        return "IntegerLiteral";
    }

    @Override
    public Type typeClass() {
        return new IntType();
    }

    @Override
    public String printPreOrder() {
        return this.toString() + "\n";
    }

    @Override
    public <E> E accept(NodeVisitor<E> visitor) {
        return visitor.visit(this);
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
        return String.format("IntegerLiteral[%d]", getIntLiteral());
    }

    public int getIntLiteral() {
        return literal;
    }

    public double getLiteral() { return literal; }
}
