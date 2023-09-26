package ast;

import coco.ArrayType;
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
        argList.add( arg );
    }

    public Symbol symbol() {
        return sym;
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("%s\n", this));

        for( String line : body.preOrderLines() ) {
            builder.append(String.format("  %s\n", line));
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("FunctionDeclaration[%s:%s]", sym.name(), sym.type()));
        // for( Symbol arg : argList ) {
        //     builder.append(String.format(", %s", arg));
        // }
        // builder.append("]");
        return builder.toString();
    }
}
