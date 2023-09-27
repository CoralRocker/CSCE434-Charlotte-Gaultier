package ast;

import coco.Token;
import coco.Symbol;

import java.util.ArrayList;

public class Designator extends AST {
    private Symbol sym;
    public Symbol getSymbol() { return sym; }
    private ArrayList<Integer> index = null;
    public ArrayList<Integer> getIndex() { return index; }

    public Designator(Token tkn, Symbol sym ) {
        super(tkn);
        this.sym = sym;
    }

    public void setIndex( ArrayList<Integer> idx ) {
        index = idx;
    }

    @Override
    public String type() {
        StringBuilder builder = new StringBuilder(sym.type().toString());
        if( index != null ) {
            for( Integer idx : index ) {
                builder.append(String.format("[%d]", idx));
            }
        }
        return builder.toString();
    }


    @Override
    public String printPreOrder() {
        return this.toString();
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("%s:%s", sym.name(), type() );
    }
}
