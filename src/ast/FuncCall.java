package ast;

import coco.Token;

public class FuncCall extends AST {
    public FuncCall(Token tkn) {
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
    public int lineNumber() {
        return 0;
    }

    @Override
    public int charPosition() {
        return 0;
    }
}
