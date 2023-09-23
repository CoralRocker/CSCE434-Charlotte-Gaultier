package ast;

public class DeclarationList extends AST {

    @Override
    public String type() {
        return "DeclList";
    }

    @Override
    public String printPreOrder() {
        return null;
    }

    @Override
    public int lineNumber() {
        return 0;
    }

    @Override
    public int charPosition() {
        return 0;
    }
}

