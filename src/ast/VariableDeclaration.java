package ast;

import coco.Symbol;
import coco.Token;

public class VariableDeclaration extends AST {

    private Symbol sym;

    public VariableDeclaration(Token tkn, Symbol sym) {
        super(tkn);
        this.sym = sym;
    }

    @Override
    public String type() {
        return "VarDecl";
    }

    @Override
    public String printPreOrder() {
        return null;
    }

}
