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
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("IntegerLiteral[%d]", getLiteral());
    }

    public int getLiteral() {
        return literal;
    }
}
