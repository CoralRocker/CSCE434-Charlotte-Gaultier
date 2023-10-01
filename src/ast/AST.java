package ast;


import coco.Token;
import types.Type;

public abstract class AST implements Visitable, IsLiteral{

    private Token tkn;

    public AST( Token tkn ) {
        this.tkn = tkn;
    }

    Type type;

    public void setType(Type t){
        this.type = t;
    };

    public abstract String type();

    public abstract Type typeClass();

    public abstract String printPreOrder();

    public String[] preOrderLines() {
        return printPreOrder().split(System.lineSeparator());
    }

    public int lineNumber() { return tkn.lineNumber(); }
    public int charPosition() { return tkn.charPosition(); }

    public Token token() { return tkn; }

    public abstract void accept(NodeVisitor visitor);

    public abstract boolean isConstEvaluable();
    public abstract AST constEvaluate();
}

