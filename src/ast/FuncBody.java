package ast;

import coco.FunctionSymbol;
import coco.Token;
import types.Type;

import java.util.ArrayList;

public class FuncBody extends AST {

    private ArrayList<Token> unresolvedSymbols;

    protected FunctionSymbol func;

    private StatSeq seq;
    private Token returnToken;
    private DeclarationList varList;
    public FuncBody(Token tkn, DeclarationList vars, StatSeq seq, FunctionSymbol func) {
        super(tkn);
        assert( vars != null );
        this.seq = seq;
        this.varList = vars;
        this.func = func;
        unresolvedSymbols = new ArrayList<>();
    }
    public void setReturnToken(Token tk){
        returnToken = tk;
    }

    public Token getReturnToken(){
        return returnToken;
    }
    public void addUnresolved( Token sym ) {
        getUnresolvedSymbols().add(sym);
    }

    public FunctionSymbol getFunc(){
        return func;
    }
    @Override
    public String type() {
        return null;
    }

    @Override
    public Type typeClass() {
        return seq.typeClass();
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
