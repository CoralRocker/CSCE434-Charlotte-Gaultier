package ast;

import coco.Token;
import types.Type;
import types.VoidType;

import java.util.ArrayList;

public class StatSeq extends AST {

    private ArrayList< AST > seq;
    private Type returnType = new VoidType();

    private Token returnToken;
    public ArrayList< AST > getSequence() { return seq; }

    public void add( AST ast ) {
        seq.add( ast );
    }
    public Token getReturnToken(){
        return returnToken;
    }

    public void setReturnToken(Token tk){
        returnToken = tk;
    }

    public StatSeq(Token tkn) {
        super(tkn);
        seq = new ArrayList<>();
    }

    @Override
    public String type() {
        return null;
    }

    public void setReturnType(Type type){
        this.returnType = type;
    }
    @Override
    public Type typeClass() {
        return returnType;
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
        return "StatementSequence";
    }
}
