package ast;

import coco.Token;

public class Power extends AST {
    private Token op;
    private AST rvalue;
    private AST lvalue;

    public Power(Token tkn, AST lvalue, AST rvalue) {
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
        return String.format("Power", op.lexeme());
    }

    public AST getRvalue() {
        return rvalue;
    }

    public AST getLvalue() {
        return lvalue;
    }
}
