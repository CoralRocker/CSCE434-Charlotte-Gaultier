package ast;

import coco.Token;

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
    public String printPreOrder() {
        return this.toString();
    }

    @Override
    public String toString() {
        return String.format("IntegerLiteral[%d]", literal);
    }
}
