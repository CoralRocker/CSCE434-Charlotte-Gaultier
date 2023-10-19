package ast;


import coco.Token;
import types.Type;

public abstract class AST implements Visitable, IsLiteral{

    Token tkn;

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

    public abstract <E> E accept(NodeVisitor<E> visitor);

    public abstract boolean isConstEvaluable();
    public abstract AST constEvaluate();

    public Token getReturnToken(){return null;};

    public String asDotGraph() { return ""; };
}

