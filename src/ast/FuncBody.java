package ast;

import coco.Token;

import java.util.ArrayList;

public class FuncBody extends AST {

    private ArrayList<Token> unresolvedSymbols;
    private StatSeq seq;
    private DeclarationList varList;
    public FuncBody(Token tkn, DeclarationList vars, StatSeq seq) {
        super(tkn);
        assert( vars != null );
        this.seq = seq;
        this.varList = vars;
        unresolvedSymbols = new ArrayList<>();
    }

    public void addUnresolved( Token sym ) {
        getUnresolvedSymbols().add(sym);
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder(this + "\n");

        if( getVarList() != null ) {
            for (String line : getVarList().preOrderLines()) {
                builder.append(String.format("  %s\n", line));
            }
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
        return "FunctionBody";
    }

    public ArrayList<Token> getUnresolvedSymbols() {
        return unresolvedSymbols;
    }

    public StatSeq getSeq() {
        return seq;
    }

    public DeclarationList getVarList() {
        return varList;
    }
}
