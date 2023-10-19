package ast;

import coco.Token;
import types.Type;

public class WhileStat extends AST {
    private AST relation;
    private StatSeq seq;
    public WhileStat(Token tkn, AST rel, StatSeq seq ) {
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
        return type;
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
        return "WhileStatement";
    }

    public AST getRelation() {
        return relation;
    }

    public StatSeq getSeq() {
        return seq;
    }
}
