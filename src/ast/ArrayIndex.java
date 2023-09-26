package ast;

import coco.Token;

public class ArrayIndex extends AST {

    private AST index;
    private AST array;

    public ArrayIndex(Token tkn, AST arr, AST idx) {
        super(tkn);
        this.array = arr;
        this.index = idx;
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder(this + "\n");
        for( String line : array.preOrderLines() ) {
            builder.append(String.format("  %s\n", line) );
        }

        for( String line : index.preOrderLines() ) {
            builder.append(String.format("  %s\n", line) );
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return "ArrayIndex";
    }
}
