package ast;

import coco.Token;

public class BoolLiteral extends AST {

    private boolean literal;
    public boolean getLiteral() { return literal; }

    public BoolLiteral(Token tkn) {
        super(tkn);
        literal = Boolean.parseBoolean(tkn.lexeme());
    }

    @Override
    public String type() {
        return "BoolLiteral";
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
        return String.format("BoolLiteral[%s]", literal);
    }
}
