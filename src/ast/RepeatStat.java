package ast;

import coco.Token;
import types.Type;

public class RepeatStat extends AST {
    private AST relation;
    private StatSeq seq;
    public RepeatStat(Token tkn, AST rel, StatSeq seq ) {
        super(tkn);
        this.relation = rel;
        this.seq = seq;
    }
    private Token returnToken;

    public void setReturnToken(Token tk){
        returnToken = tk;
    }

    public Token getReturnToken(){
        return returnToken;
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


        for( String line : getSeq().preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        // builder.append("  Until\n");
        for( String line : getRelation().preOrderLines() ) {
            builder.append(String.format("    %s\n", line));
        }

        return builder.toString();

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

    @Override
    public String toString() {
        return "RepeatStatement";
    }

    public AST getRelation() {
        return relation;
    }

    public StatSeq getSeq() {
        return seq;
    }
}
