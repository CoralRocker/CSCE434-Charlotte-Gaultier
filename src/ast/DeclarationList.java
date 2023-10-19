package ast;

import coco.Token;
import types.AryType;
import types.Type;

import java.util.ArrayList;
import java.util.Collection;

public class DeclarationList extends AST {

    public DeclarationList(Token tkn) {
        super(tkn);
        symbols = new ArrayList<>();
    }

    public DeclarationList(Token tkn, ArrayList<AST> vars) {
        super(tkn);
        assert( vars != null );
        this.symbols = vars;
    }

    public <T extends AST> void add( T decl ) {
        symbols.add( decl );
    }
    public void addAll(ArrayList<? extends AST> vars ) {
        symbols.addAll(vars);
    }

    private ArrayList<AST> symbols;

    public ArrayList<AST> getSymbols() { return symbols; }

    @Override
    public String type() {
        return "DeclList";
    }

    @Override
    public Type typeClass() {
        return null;
    }

    @Override
    public String printPreOrder() {
        StringBuilder ret = new StringBuilder();

        ret.append( this );
        ret.append("\n");
        for( AST var : symbols ) {
            for( String line : var.preOrderLines() ) {
                ret.append(String.format("  %s\n", line));
            }
        }

        return ret.toString();
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

    public ArrayList<AST> getContained() {
        return symbols;
    }

    @Override
    public String toString() {
        return String.format("DeclarationList");
    }
}

