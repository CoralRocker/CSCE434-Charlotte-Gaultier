package ast;

import coco.Token;
import coco.Variable;

import java.util.ArrayList;
import java.util.List;

public class DeclarationList extends AST {

    public DeclarationList(Token tkn) {
        super(tkn);
        symbols = new ArrayList<>();
    }

    public DeclarationList(Token tkn, ArrayList<VariableDeclaration> vars) {
        super(tkn);
        this.symbols = vars;
    }

    public void add( VariableDeclaration decl ) {
        symbols.add( decl );
    }
    public void addAll( List<VariableDeclaration> vars ) {
        symbols.addAll(vars);
    }

    private ArrayList<VariableDeclaration> symbols;

    @Override
    public String type() {
        return "DeclList";
    }

    @Override
    public String printPreOrder() {
        StringBuilder ret = new StringBuilder();

        ret.append( this );
        ret.append("\n");
        for( VariableDeclaration var : symbols ) {
            ret.append("  ");
            ret.append(var);
            ret.append("\n");
        }

        return ret.toString();
    }

    @Override
    public String toString() {
        return String.format("ast.DeclarationList(%d,%d)", lineNumber(), charPosition());
    }
}

