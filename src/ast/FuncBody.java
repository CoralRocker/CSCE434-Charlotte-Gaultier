package ast;

import coco.Token;

public class FuncBody extends AST {

    private StatSeq seq;
    private DeclarationList varList;
    public FuncBody(Token tkn, DeclarationList vars, StatSeq seq) {
        super(tkn);
        this.seq = seq;
        this.varList = vars;
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder(this + "\n");

        for( String line : seq.preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return "FunctionBody";
    }
}
