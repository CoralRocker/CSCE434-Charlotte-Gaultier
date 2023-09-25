package ast;

import coco.Token;
import coco.Variable;

import java.util.ArrayList;

public class DeclarationList extends AST {

    public DeclarationList(Token tkn) {
        super(tkn);
        symbols = new ArrayList<>();
    }

    public void add( VariableDeclaration decl ) {
        symbols.add( decl );
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
            ret.append("\t");
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

