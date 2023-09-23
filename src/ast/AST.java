package ast;


public abstract class AST {
    public abstract String type();

    public abstract String printPreOrder();

    public abstract int lineNumber();
    public abstract int charPosition();
}

