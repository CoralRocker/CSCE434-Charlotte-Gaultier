package ast;

import coco.Token;

public class WhileStat extends AST {
    private AST relation;
    private StatSeq seq;
    public WhileStat(Token tkn, AST rel, StatSeq seq ) {
        super(tkn);
        this.relation = rel;
        this.seq = seq;
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder(this + "\n");

        for( String line : relation.preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        for( String line : seq.preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return "WhileStatement";
    }
}
