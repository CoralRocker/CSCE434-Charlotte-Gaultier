package ast;

import coco.*;
import types.Type;

public class ArrayIndex extends AST implements Visitable {

    private AST index;

    public AST getIndex() { return index; }
    private AST array;
    public AST getArray() { return array; }

    public final Token endBrace;

    private Symbol sym;

    public void setSymbol(Symbol sym){
        this.sym = sym;
    }

    public Symbol getSymbol(){
        return sym;
    }

    public ArrayIndex(Token tkn, Token endBrace, AST arr, AST idx, ArrayType type) {
        super(tkn);
        this.array = arr;
        this.index = idx;
        this.endBrace = endBrace;
//        this.sym = new ArraySymbol(this.toString(), type, idx.);
    }

    public Token getIdentToken() {
        if( array instanceof ArrayIndex ) {
            return ((ArrayIndex) array).getIdentToken();
        }
        return array.token();
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public Type typeClass() {
        return type;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder(this + "\n");
        for( String line : array.preOrderLines() ) {
            builder.append(String.format("  %s\n", line) );
        }

        // for( String line : index.preOrderLines() ) {
        //     builder.append(String.format("  %s\n", line) );
        // }

        return builder.toString();
    }

    @Override
    public String toString() {
        String arr = array.toString();
        String ind = index.toString();
        if(array instanceof Designator){
            arr = ((Designator) array).getSymbol().name();
        }
        if(index instanceof Designator){
            ind = ((Designator) index).getSymbol().name();
        }
        return arr + "[" + ind + "]";
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

}
