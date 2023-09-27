package ast;

import coco.Token;

public class Relation extends AST {

    private AST rvalue;
    private AST lvalue;
    private Token op;

    public Relation(Token tkn, AST lval, AST rval) {
        super(tkn);
        op = tkn;
        rvalue = rval;
        lvalue = lval;
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
        String[] lines = getLvalue().printPreOrder().split(System.lineSeparator());
        for( String line : lines ) {
            builder.append(String.format("  %s\n", line));
        }
        lines = getRvalue().printPreOrder().split(System.lineSeparator());
        for( String line : lines ) {
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
        return String.format("Relation[%s]", op.lexeme());
    }

    public AST getRvalue() {
        return rvalue;
    }

    public AST getLvalue() {
        return lvalue;
    }
}
