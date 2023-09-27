package ast;

import coco.Symbol;
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
        unresolvedSymbols.add(sym);
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder(this + "\n");

        if( varList != null ) {
            for (String line : varList.preOrderLines()) {
                builder.append(String.format("  %s\n", line));
            }
        }

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
