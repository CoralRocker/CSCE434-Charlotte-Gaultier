package ast;

import coco.Token;

public class ArrayIndex extends AST implements Visitable {

    private AST index;

    public AST getIndex() { return index; }
    private AST array;
    public AST getArray() { return array; }

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
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
