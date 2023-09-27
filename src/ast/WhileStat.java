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

        for( String line : getRelation().preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        for( String line : getSeq().preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        return builder.toString();
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "WhileStatement";
    }

    public AST getRelation() {
        return relation;
    }

    public StatSeq getSeq() {
        return seq;
    }
}
