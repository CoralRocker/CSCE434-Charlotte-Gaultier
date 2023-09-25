package ast;

import coco.Token;
import coco.Variable;

import java.util.ArrayList;

public class DeclarationList extends AST {

    public DeclarationList(Token tkn) {
        super(tkn);
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
        return null;
    }
}

