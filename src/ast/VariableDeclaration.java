package ast;

public class VariableDeclaration extends AST {

    @Override
    public String type() {
        return "VarDecl";
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
