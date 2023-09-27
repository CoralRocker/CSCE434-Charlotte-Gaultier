package ast;

import coco.Token;

public class RelExpr extends AST {
    public RelExpr(Token tkn) {
        super(tkn);
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public String printPreOrder() {
        return null;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
