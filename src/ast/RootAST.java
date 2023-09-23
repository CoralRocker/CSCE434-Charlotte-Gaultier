package ast;

public class RootAST extends AST {

    public RootAST() {
    }

    @Override
    public String type() {
        return "Computation";
    }

    @Override
    public String printPreOrder() {
        return "NOT IMPLEMENTED";
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
