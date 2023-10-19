package ast;

import coco.Symbol;
import coco.Token;
import types.Type;

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
    public Type typeClass() {
        return null;
    }

    @Override
    public String printPreOrder() {
        return this + "\n";
    }

    @Override
    public <E> E accept(NodeVisitor<E> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean isConstEvaluable() {
        return false;
    }

    @Override
    public AST constEvaluate() {
        return null;
    }

    @Override
    public String toString() {
        return String.format("VariableDeclaration[%s:%s]", sym.name(), sym.type());
    }
}
