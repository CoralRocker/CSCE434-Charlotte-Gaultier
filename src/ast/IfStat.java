package ast;

import coco.Token;

public class IfStat extends AST {

    public StatSeq ifseq, elseseq;
    public Relation ifrel;

    public IfStat(Token tkn, Relation ifrel, StatSeq ifseq, StatSeq elseseq) {
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
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder(this + "\n");

        for( String line : ifrel.preOrderLines() ) {
            builder.append(String.format("\t%s\n", line));
        }

        for( String line : ifseq.preOrderLines() ) {
            builder.append(String.format("\t%s\n", line));
        }

        if( elseseq != null ) {
            for (String line : elseseq.preOrderLines()) {
                builder.append(String.format("\t%s\n", line));
            }
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return "IfStatement";
    }
}
