package ast;

import coco.Symbol;
import coco.Token;

import java.util.ArrayList;

public class FuncDecl extends AST {

    private Symbol sym;
    private ArrayList<Symbol> argList;
    private FuncBody body;
    public FuncDecl(Token tkn, Symbol func, FuncBody body ) {
        super(tkn);
        this.sym = func;
        this.body = body;
        this.argList = new ArrayList<>();
    }

    public void setArgs( ArrayList<Symbol> list) {
        this.argList = list;
    }

    public void add( Symbol arg ) {
        getArgList().add( arg );
    }

    public Symbol symbol() {
        return getSym();
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("%s\n", this));

        for( String line : getBody().preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        return builder.toString();
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("FunctionDeclaration[%s:%s]", getSym().name(), getSym().type()));
        // for( Symbol arg : argList ) {
        //     builder.append(String.format(", %s", arg));
        // }
        // builder.append("]");
        return builder.toString();
    }

    public Symbol getSym() {
        return sym;
    }

    public ArrayList<Symbol> getArgList() {
        return argList;
    }

    public FuncBody getBody() {
        return body;
    }
}
