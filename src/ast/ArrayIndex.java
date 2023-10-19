package ast;

import coco.Token;
import types.Type;

public class ArrayIndex extends AST implements Visitable {

    private AST index;

    public AST getIndex() { return index; }
    private AST array;
    public AST getArray() { return array; }

    public final Token endBrace;


    public ArrayIndex(Token tkn, Token endBrace, AST arr, AST idx) {
        super(tkn);
        this.array = arr;
        this.index = idx;
        this.endBrace = endBrace;
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
        return "ArrayIndex";
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
