package ast;

import coco.Symbol;
import coco.Token;

public class VariableDeclaration extends AST {

    private Symbol sym;

    public VariableDeclaration(Token tkn, Symbol sym) {
        super(tkn);
        this.sym = sym;
    }

    public Symbol symbol() {
        return sym;
    }

    @Override
    public String type() {
        return "VarDecl";
    }

    @Override
    public String printPreOrder() {
        return null;
    }

    @Override
    public String toString() {
        return String.format("ast.VariableDeclaration(%d,%d)[%s]", lineNumber(), charPosition(), sym);
    }
}
