package ast;

import coco.Token;
import types.Type;

import java.util.ArrayList;

public class StatSeq extends AST {

    private ArrayList< AST > seq;

    public ArrayList< AST > getSequence() { return seq; }

    public void add( AST ast ) {
        seq.add( ast );
    }

    public StatSeq(Token tkn) {
        super(tkn);
        seq = new ArrayList<>();
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

        for( AST ast : seq ) {
            for( String line : ast.preOrderLines() ) {
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
    public boolean isConstEvaluable() {
        return false;
    }

    @Override
    public AST constEvaluate() {
        return null;
    }

    @Override
    public String toString() {
        return "StatementSequence";
    }
}
