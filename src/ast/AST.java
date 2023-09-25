package ast;


import coco.Token;

public abstract class AST {

    private int line;
    private int charpos;

    public AST( Token tkn ) {
        line = tkn.lineNumber();
        charpos = tkn.charPosition();
    }


    public abstract String type();

    public abstract String printPreOrder();

    public String[] preOrderLines() {
        return printPreOrder().split(System.lineSeparator());
    }

    public int lineNumber() { return line; }
    public int charPosition() { return charpos; }
}

