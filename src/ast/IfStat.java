package ast;

import coco.Token;
import types.Type;

public class IfStat extends AST {

    private StatSeq ifseq;
    private StatSeq elseseq;
    private AST ifrel;

    public IfStat(Token tkn, AST ifrel, StatSeq ifseq, StatSeq elseseq) {
        super(tkn);
        this.ifrel = ifrel;
        this.ifseq = ifseq;
        this.elseseq = elseseq;
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public Type typeClass() {
        return null;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder(this + "\n");

        for( String line : getIfrel().preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        for( String line : getIfseq().preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        if( getElseseq() != null ) {
            for (String line : getElseseq().preOrderLines()) {
                builder.append(String.format("  %s\n", line));
            }
        }

        return builder.toString();
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "IfStatement";
    }

    public StatSeq getIfseq() {
        return ifseq;
    }

    public StatSeq getElseseq() {
        return elseseq;
    }

    public AST getIfrel() {
        return ifrel;
    }
}
