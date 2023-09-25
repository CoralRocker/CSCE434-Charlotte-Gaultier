package ast;

import coco.Token;
import coco.Symbol;

public class Designator extends AST {
    private Symbol sym;
    public Designator(Token tkn, Symbol sym ) {
        super(tkn);
        this.sym = sym;
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public String printPreOrder() {
        return this.toString();
    }

    @Override
    public String toString() {
        return String.format("%s:%s", sym.name(), sym.type() );
    }
}
