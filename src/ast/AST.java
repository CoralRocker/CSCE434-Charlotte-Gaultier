package ast;


import coco.Token;

public abstract class AST implements Visitable {

    private Token tkn;

    public AST( Token tkn ) {
        this.tkn = tkn;
    }


    public abstract String type();

    public abstract String printPreOrder();

    public String[] preOrderLines() {
        return printPreOrder().split(System.lineSeparator());
    }

    public int lineNumber() { return tkn.lineNumber(); }
    public int charPosition() { return tkn.charPosition(); }

    public Token token() { return tkn; }

    public abstract void accept(NodeVisitor visitor);
}

